package similarity

import NetGraphAlgebraDefs.NodeObject
import org.apache.logging.log4j.{LogManager, Logger}

object SimilarityMetrics {
  val logger: Logger = LogManager.getLogger(getClass.getName)
  def normalizeAttributes(node: NodeObject, minMaxValues: ((Int, Int), (Int, Int), (Int, Int), (Int, Int), (Int, Int), (Int, Int), (Int, Int), (Double, Double))): Array[Double] = {
    val attributes = Array(
      node.children.toDouble,
      node.props.toDouble,
      node.currentDepth.toDouble,
      node.propValueRange.toDouble,
      node.maxDepth.toDouble,
      node.maxBranchingFactor.toDouble,
      node.maxProperties.toDouble,
      node.storedValue
    )
    logger.info(s"Inside normalize Attributes")

    val minMaxArray = Array(
      (minMaxValues._1._1.toDouble, minMaxValues._1._2.toDouble),
      (minMaxValues._2._1.toDouble, minMaxValues._2._2.toDouble),
      (minMaxValues._3._1.toDouble, minMaxValues._3._2.toDouble),
      (minMaxValues._4._1.toDouble, minMaxValues._4._2.toDouble),
      (minMaxValues._5._1.toDouble, minMaxValues._5._2.toDouble),
      (minMaxValues._6._1.toDouble, minMaxValues._6._2.toDouble),
      (minMaxValues._7._1.toDouble, minMaxValues._7._2.toDouble),
      minMaxValues._8
    )


    attributes.zipWithIndex.map { case (value, index) =>
      val (min, max) = minMaxArray(index)
      if (min == max) 0.5 // Handle constant values
      else (value - min) / (max - min)
    }
  }

  def calculateDistance(attributes1: Array[Double], attributes2: Array[Double]): Double = {
    logger.info(s"Invoking calculate distance")
    attributes1.zip(attributes2).map { case (a, b) => math.abs(a - b) }.sum
  }

  def calculateSimilarity(node1: NodeObject, node2: NodeObject, minMaxValues: ((Int, Int), (Int, Int), (Int, Int), (Int, Int), (Int, Int), (Int, Int), (Int, Int), (Double, Double))): Double = {
    logger.info(s"calculating similarity")
    val normalizedAttributes1 = normalizeAttributes(node1, minMaxValues)
    val normalizedAttributes2 = normalizeAttributes(node2, minMaxValues)

    val distance = calculateDistance(normalizedAttributes1, normalizedAttributes2)

    // If the valuableData attribute is different, increase the distance
    if (node1.valuableData != node2.valuableData) distance + 1 else distance
  }

  def convertDistanceToSimilarity(distance: Double): Double = {
    logger.info(s"convert Distance to similarity")
    1 / (1 + distance)
  }

}
