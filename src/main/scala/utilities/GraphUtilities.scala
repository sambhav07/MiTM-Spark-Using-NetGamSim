package utilities

import org.yaml.snakeyaml.Yaml

import java.io.{ByteArrayInputStream, FileInputStream, InputStream}
import scala.io.Source
import scala.collection.JavaConverters._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import org.apache.spark.graphx._
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, PutObjectRequest}
import software.amazon.awssdk.regions.Region
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.methods.{HttpGet, HttpPut}
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import similarity.SimilarityMetrics.logger
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody

import java.net.URL

object GraphUtilities {

  def isS3Url(url: String): Boolean = {
    url.matches("^https://[^.]+\\.s3\\.amazonaws\\.com/.*$")
  }

  def writeContentToS3(bucket: String, key: String, content: String, region: Region = Region.US_EAST_1): Unit = {
    val s3Client = S3Client.builder()
      .region(region)
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATJ7MSUGHDGZSJH52", "pFnuAFFB9jGN6njE3uct71hNNzmNSsWpbnZsvKm1")))
      .build()
    s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
      RequestBody.fromString(content))
    s3Client.close()
  }

  def writeContentToHttp(url: String, content: String): Unit = {
    val httpClient: CloseableHttpClient = HttpClients.createDefault()
    println(s"url: ${url}")
    println(s"content: ${content}")
    val httpPut = new HttpPut(url)
    println(s"httpPut: ${httpPut}")
    httpPut.setEntity(new StringEntity(content, "UTF-8"))
    httpClient.execute(httpPut)
    httpClient.close()
  }

  def writeYamlContentToHttp(url: String, content: String): Unit = {
    logger.info(s"writing Yaml file through http")
    val httpClient: CloseableHttpClient = HttpClients.createDefault()
    println(s"url: ${url}")
    println(s"content: ${content}")
    val httpPut = new HttpPut(url)
    println(s"httpPut: ${httpPut}")
    httpPut.setEntity(new StringEntity(content, "UTF-8"))

    val response = httpClient.execute(httpPut)
    println(s"HTTP Response Status: ${response.getStatusLine}")
    val entity = response.getEntity
    if (entity != null) {
      val responseString = EntityUtils.toString(entity)
      println(s"HTTP Response Body: ${responseString}")
    }
    httpClient.close()
  }


  def replaceTabsWithSpaces(inputStream: InputStream, outputFilePath: String, spacesPerTab: Int = 2): Unit = {
    logger.info(s"replacing tabs with spaces")
    val content = Source.fromInputStream(inputStream).mkString.replaceAll("\t", " " * spacesPerTab)

    if (outputFilePath.startsWith("s3://")) {
      val uriParts = outputFilePath.stripPrefix("s3://").split("/", 2)
      val bucket = uriParts(0)
      val key = uriParts(1)
      writeContentToS3(bucket, key, content)
    } else if (isS3Url(outputFilePath)) {
      val uriParts = outputFilePath.stripPrefix("https://").split("/", 2)
      val bucket = uriParts(0).split("\\.")(0)
      val key = uriParts(1)
      writeContentToS3(bucket, key, content)
    } else if (outputFilePath.startsWith("http://") || outputFilePath.startsWith("https://")) {
      writeContentToHttp(outputFilePath, content)
    } else {
      val writer = new java.io.PrintWriter(outputFilePath)
      writer.write(content)
      writer.close()
    }
  }

  def processYamlFile(inputYamlPath: String, fixedYamlPath: String): (List[Any], List[Any], List[Any]) = {
    logger.info(s"processing yaml file")

    val inputStream: InputStream =
      if (inputYamlPath.startsWith("s3://")) {
      val s3Client = S3Client.builder()
        .region(Region.of("us-east-1"))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATJ7MSUGHDGZSJH52", "pFnuAFFB9jGN6njE3uct71hNNzmNSsWpbnZsvKm1")))
        .build()

        val uriParts = inputYamlPath.stripPrefix("s3://").split("/", 2)
        val bucket = uriParts(0)
        val key = uriParts(1)
        val getRequest = GetObjectRequest.builder().bucket(bucket).key(key).build()
        s3Client.getObject(getRequest)
      } else if (inputYamlPath.startsWith("http://") || inputYamlPath.startsWith("https://")) {
        val httpClient: CloseableHttpClient = HttpClients.createDefault()
        val httpGet = new HttpGet(inputYamlPath)
        val httpResponse = httpClient.execute(httpGet)
        httpResponse.getEntity.getContent
      } else {
        new FileInputStream(inputYamlPath)
      }

    replaceTabsWithSpaces(inputStream, fixedYamlPath)

    val yaml = new Yaml()
    val fixedYamlInputStream: InputStream =
    if (fixedYamlPath.startsWith("s3://") || fixedYamlPath.startsWith("https://")) {
      val s3Client = S3Client.builder()
        .region(Region.of("us-east-1"))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATJ7MSUGHDGZSJH52", "pFnuAFFB9jGN6njE3uct71hNNzmNSsWpbnZsvKm1")))
        .build()
      println(s"fixedYamlPath: ${fixedYamlPath}")

      val uriWithoutScheme = if (fixedYamlPath.startsWith("s3://")) {
        fixedYamlPath.stripPrefix("s3://")
      } else {
        new URL(fixedYamlPath).getHost + new URL(fixedYamlPath).getPath
      }

      val uriParts = uriWithoutScheme.split("/", 2)
      val bucket = uriParts(0).split("\\.")(0)
      println(s"bucket: ${bucket}")
      val key = uriParts(1)
      println(s"key: ${key}")

      val getRequest = GetObjectRequest.builder().bucket(bucket).key(key).build()
      println(s"getRequest: ${getRequest}")
      s3Client.getObject(getRequest)
    } else {
      new FileInputStream(fixedYamlPath)
    }


  val yamlData = yaml.loadAs(fixedYamlInputStream, classOf[java.util.Map[String, Any]])
  fixedYamlInputStream.close()

    val nodesData = Option(yamlData.get("Nodes"))
      .getOrElse(new java.util.HashMap[String, Any]())
      .asInstanceOf[java.util.Map[String, Any]]

    val modifiedNodes = Option(nodesData.get("Modified"))
      .getOrElse(new java.util.ArrayList[Any]())
      .asInstanceOf[java.util.List[Any]]
      .asScala.toList

    val removedNodes = Option(nodesData.get("Removed"))
      .getOrElse(new java.util.ArrayList[Any]())
      .asInstanceOf[java.util.List[Any]]
      .asScala.toList

    val addedNodes = Option(nodesData.get("Added"))
      .getOrElse(new java.util.HashMap[String, Any]())
      .asInstanceOf[java.util.Map[String, Any]]
      .values
      .asScala
      .toList

    inputStream.close()
    (modifiedNodes, removedNodes, addedNodes)
  }


  def writeStatsToFile(
                        filename: String,
                        optimalNumWalks: Int,
                        numberOfSuccessfulAttacks: Int,
                        numberOfFailedAttacks: Int,
                        failedAttacks: Set[(VertexId, VertexId, Double)],
                        successfulAttacks: Set[(VertexId, VertexId, Double)],
                        metrics: Map[String, Double] // <-- Add this parameter
                      ): Unit = {
    logger.info(s"writing statistics to file")
    val sb = new StringBuilder()

    // Write the summary statistics
    sb.append(s"Optimal Number of Walks: $optimalNumWalks\n")
    sb.append(s"Number of Successful Attacks: $numberOfSuccessfulAttacks\n")
    sb.append(s"Number of Failed Attacks: $numberOfFailedAttacks\n\n")

    // Write header and failed attacks
    sb.append("---- Failed Attacks ----\n")
    failedAttacks.foreach { case (nodeId, valuableNodeId, similarityScore) =>
      sb.append(s"Node: $nodeId, Valuable Node: $valuableNodeId, Similarity Score: $similarityScore\n")
    }

    // Space and then successful attacks
    sb.append("\n---- Successful Attacks ----\n")
    successfulAttacks.foreach { case (nodeId, valuableNodeId, similarityScore) =>
      sb.append(s"Node: $nodeId, Valuable Node: $valuableNodeId, Similarity Score: $similarityScore\n")
    }

    // Write metrics   <-- Add this block
    sb.append("\n---- Metrics ----\n")
    metrics.foreach { case (metricName, metricValue) =>
      sb.append(s"$metricName: $metricValue\n")
    }

    if (filename.startsWith("s3://")) {
      val uriParts = filename.stripPrefix("s3://").split("/", 2)
      val bucket = uriParts(0)
      val key = uriParts(1)
      writeContentToS3(bucket, key, sb.toString())
    } else if (filename.startsWith("http://") || filename.startsWith("https://")) {
      writeYamlContentToHttp(filename, sb.toString())
    } else {
      Files.write(Paths.get(filename), sb.toString().getBytes(StandardCharsets.UTF_8))
    }
  }


}
