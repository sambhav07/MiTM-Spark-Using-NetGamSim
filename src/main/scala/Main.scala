import NetGraphAlgebraDefs.NodeObject
import org.apache.logging.log4j.{LogManager, Logger}
import org.apache.spark.graphx._
import org.apache.spark.{SparkConf, SparkContext}
import similarity.SimilarityMetrics._
import utilities.GraphUtilities._
import utilities.NetGraph.loadGraph

import scala.collection.mutable.ListBuffer
import scala.util.Random


object Main {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("RandomWalkGraphX").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val logger: Logger = LogManager.getLogger(getClass.getName)

    val originalNodesObjectsURL = args(0)
    val perturbedNodesObjectsURL = args(1)

    logger.info(s"arg 0 : ${originalNodesObjectsURL}")
    logger.info(s"arg 1 : ${perturbedNodesObjectsURL}")

    val (originalNodesObjects, _) = loadGraph(originalNodesObjectsURL)
    val (perturbedNodesObjects, perturbedEdgesObject) = loadGraph(perturbedNodesObjectsURL)
    var successfulWalks = 0

    logger.info(s"perturbed nodes: ${perturbedNodesObjects}")
    logger.info(s"perturbed edges: ${perturbedEdgesObject}")

    logger.info(s"original nodes: ${originalNodesObjects}")

    val nodesWithValuableData = originalNodesObjects.filter(node => node.valuableData)

    logger.info(s"nodes with valueable data: ${nodesWithValuableData}")

    val allNodes = originalNodesObjects ++ perturbedNodesObjects

    // Extracting min and max for each attribute
    val minMaxValues = (
      (allNodes.minBy(_.children).children, allNodes.maxBy(_.children).children),
      (allNodes.minBy(_.props).props, allNodes.maxBy(_.props).props),
      (allNodes.minBy(_.currentDepth).currentDepth, allNodes.maxBy(_.currentDepth).currentDepth),
      (allNodes.minBy(_.propValueRange).propValueRange, allNodes.maxBy(_.propValueRange).propValueRange),
      (allNodes.minBy(_.maxDepth).maxDepth, allNodes.maxBy(_.maxDepth).maxDepth),
      (allNodes.minBy(_.maxBranchingFactor).maxBranchingFactor, allNodes.maxBy(_.maxBranchingFactor).maxBranchingFactor),
      (allNodes.minBy(_.maxProperties).maxProperties, allNodes.maxBy(_.maxProperties).maxProperties),
      (allNodes.minBy(_.storedValue).storedValue, allNodes.maxBy(_.storedValue).storedValue)
    )

    logger.info(s"minMaxValues: ${minMaxValues}")

    // A cache for storing computed similarity scores
    val similarityCache = scala.collection.mutable.Map[(Long, Long), Double]()

    val failedAttackDetails = scala.collection.mutable.ListBuffer[(Long, Long, Double)]()
    val successfulAttackDetails = scala.collection.mutable.ListBuffer[(Long, Long, Double)]()

    val inputYamlPath = args(2)
    val fixedYamlPath = args(3)


    val (modifiedNodes, removedNodes, addedNodes) = processYamlFile(inputYamlPath, fixedYamlPath)
    logger.info(s"Modified Nodes: $modifiedNodes")
    logger.info(s"Removed Nodes: $removedNodes")
    logger.info(s"Added Nodes: $addedNodes")



    //construct graph using graphx library and spark context
    val vertices = sc.parallelize(perturbedNodesObjects.map(node => (node.id.toLong, node)))
    val edges = sc.parallelize(perturbedEdgesObject.map(action => Edge(action.fromNode.id.toLong, action.toNode.id.toLong, action)))
    val graph = Graph(vertices, edges)

    def randomWalkFromNode[T](graph: Graph[T, _], startNode: VertexId, maxSteps: Int): List[VertexId] = {
      logger.info(s"Starting Random walk from node")
      var currentNode = startNode
      var walkPath = List[VertexId](currentNode)
      var visitedNodes = Set[VertexId](currentNode)

      for (_ <- 1 to maxSteps) {
        val neighbors = graph.edges.filter(e => e.srcId == currentNode).map(e => e.dstId).collect().filterNot(visitedNodes.contains)
        if (neighbors.isEmpty) {
          return walkPath
        }

        currentNode = neighbors(Random.nextInt(neighbors.length))
        visitedNodes += currentNode
        walkPath = walkPath :+ currentNode
      }

      walkPath
    }

    def randomWalk[T](graph: Graph[T, _], maxSteps: Int, alreadyStartedNodes: Set[VertexId]): (List[VertexId], Set[VertexId]) = {
      logger.info(s"Inside RandomWalk method")
      val remainingNodes = graph.vertices.filter(v => !alreadyStartedNodes.contains(v._1)).collect()

      // If we've already started from every node, return empty
      if (remainingNodes.isEmpty) {
        return (List(), Set())
      }

      // Pick a random node from the remaining nodes
      val randomStartNode = remainingNodes(Random.nextInt(remainingNodes.length))._1

      (randomWalkFromNode(graph, randomStartNode, maxSteps), alreadyStartedNodes + randomStartNode)
    }

    def decideNumWalksAndShowPaths[T](graph: Graph[T, _], desiredCoverage: Double): (Int, Map[String, Double]) = {
      logger.info(s"Random walk initiated")
      val maxWalks = graph.vertices.count().toInt * 2 // Just keeping this factor of 2. Adjust if needed.
      var currentWalks = 0
      var currentCoverage = 0.0
      var visitedNodes = Set[VertexId]()
      var alreadyStartedNodes = Set[VertexId]()
      val allPathsStringBuilder = new StringBuilder()
      val pathLengths = ListBuffer[Int]()

      val baseSteps = 5 // Minimum steps.
      val maxSteps = baseSteps + math.ceil(math.log10(graph.vertices.count() + 1)).toInt

      while (currentCoverage < desiredCoverage && currentWalks < maxWalks) {
        val (path, updatedStartedNodes) = randomWalk(graph, maxSteps, alreadyStartedNodes)
        alreadyStartedNodes = updatedStartedNodes

        allPathsStringBuilder ++= s"Random Walk ${currentWalks + 1}: " + path.mkString(" -> ") + "\n"
        visitedNodes ++= path
        var walkSuccessful = false

        // Add the length of the current path to the list of path lengths
        pathLengths += path.length

        path.foreach { nodeIdInPath =>
          val nodeInPath = graph.vertices.filter(v => v._1 == nodeIdInPath).first()._2.asInstanceOf[NodeObject]
          nodesWithValuableData.foreach { valuableNode =>
            val nodePair = (nodeInPath.id.toLong, valuableNode.id.toLong)

            // If the score for the current pair is not in the cache, compute and potentially store it
            if (!similarityCache.contains(nodePair)) {
              val similarityScore = calculateSimilarity(nodeInPath, valuableNode, minMaxValues)
              val convertedScore = convertDistanceToSimilarity(similarityScore)

              // Print the converted score for checking
              println(s"Converted Similarity Score between Node: ${nodeInPath.id} and Valuable Node: ${valuableNode.id} is: $convertedScore")

              // Check if the score exceeds the threshold for an attack
              if (convertedScore > 0.7) {
                val attackDetails = (nodeIdInPath, valuableNode.id.toLong, convertedScore)
                if (addedNodes.contains(nodeIdInPath) || modifiedNodes.contains(valuableNode.id)) {
                  // Failed attack: Store the node ID, valuable node ID, and their similarity score
                  if (!failedAttackDetails.contains(attackDetails)) {
                    failedAttackDetails += attackDetails
                  }
                } else {
                  walkSuccessful = true
                  // Successful attack: Store the node ID, valuable node ID, and their similarity score
                  if (!successfulAttackDetails.contains(attackDetails)) {
                    successfulAttackDetails += attackDetails
                  }
                }
              }

              // Cache the score only if it's greater than or equal to 0.8
              if (convertedScore >= 0.8) {
                similarityCache(nodePair) = convertedScore
              }
            }

            // If the converted similarity is in the cache (meaning it's >= 0.8), print it
            similarityCache.get(nodePair).foreach { convertedSimilarity =>
              println(s"Similarity between Node: ${nodeInPath.id} and Valuable Node: ${valuableNode.id} is: $convertedSimilarity")
            }
          }
        }

        currentWalks += 1

        val newCoverage = visitedNodes.size.toDouble / graph.vertices.count()
        println(s"Random Walk ${currentWalks}: ${path.mkString(" -> ")} | Coverage: ${newCoverage * 100}%")
        currentCoverage = newCoverage
        if (walkSuccessful) {
          successfulWalks += 1
        }
      }

      println("\n---- Similarity Cache ----")
      similarityCache.foreach { case ((nodeId, valuableNodeId), similarityScore) =>
//        println(s"Node: $nodeId, Valuable Node: $valuableNodeId, Similarity Score: $similarityScore")
        logger.info(s"Node: $nodeId, Valuable Node: $valuableNodeId, Similarity Score: $similarityScore")

      }

      // Metrics calculation and printing
      val sortedPathLengths = pathLengths.sorted
      val minLength = sortedPathLengths.head
      val maxLength = sortedPathLengths.last
      val medianLength = if (sortedPathLengths.size % 2 == 0) {
        (sortedPathLengths(sortedPathLengths.size / 2 - 1) + sortedPathLengths(sortedPathLengths.size / 2)) / 2.0
      } else {
        sortedPathLengths(sortedPathLengths.size / 2)
      }
      val meanLength = pathLengths.sum.toDouble / pathLengths.size
      logger.info(s"Min Length: $minLength, Max Length: $maxLength, Median Length: $medianLength, Mean Length: $meanLength")

      val successWalksRatio = successfulWalks.toDouble / currentWalks
      logger.info(s"Success Walks and Total Walks: ${successfulWalks.toDouble} $currentWalks")
      logger.info(s"Success Walks Ratio: $successWalksRatio")
      println(allPathsStringBuilder.toString())

      val metricsMap = Map(
        "Total Walks " -> currentWalks.toDouble,
        "Minimum no of Nodes in total walks " -> minLength.toDouble,
        "Max no of Nodes in total walks " -> maxLength.toDouble,
        "Median no of Nodes in total walks " -> medianLength.toDouble,
        "Mean no of Nodes in total walks " -> meanLength,
        "Successful Walks " -> successfulWalks.toDouble,
        "Success Walks Ratio " -> successWalksRatio
      )

      (currentWalks, metricsMap)
    }

    val desiredCoverage = 0.9
    val (optimalNumWalks, metrics) = decideNumWalksAndShowPaths(graph, desiredCoverage)
    logger.info(s"Optimal number of walks for desired coverage: $optimalNumWalks")
    val numberOfSuccessfulAttacks = successfulAttackDetails.length
    val numberOfFailedAttacks = failedAttackDetails.length

    // Print number of successful attacks
    logger.info(s"Number of Successful Attacks: $numberOfSuccessfulAttacks")
    // Print details of successful attacks
    successfulAttackDetails.foreach { case (nodeId, valuableId, score) =>
      logger.info(s"Successful Attack Detail - Node: $nodeId, Valuable Node: $valuableId, Similarity Score: $score")
    }

    // Print number of failed attacks
    println(s"\nNumber of Failed Attacks: $numberOfFailedAttacks")
    logger.info(s"\n logger Number of Failed Attacks: $numberOfFailedAttacks")
    // Print details of failed attacks
    failedAttackDetails.foreach { case (nodeId, valuableId, score) =>
      logger.info(s"Failed Attack Detail - Node: $nodeId, Valuable Node: $valuableId, Similarity Score: $score")
    }

    val failedAttacksSet = failedAttackDetails.toSet
    val successfulAttacksSet = successfulAttackDetails.toSet
    val filename = args(4)
    writeStatsToFile(filename, optimalNumWalks, numberOfSuccessfulAttacks, numberOfFailedAttacks, failedAttacksSet, successfulAttacksSet, metrics)
  }
}