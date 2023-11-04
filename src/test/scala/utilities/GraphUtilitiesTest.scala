package utilities

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream, FileNotFoundException}
import java.nio.charset.StandardCharsets
import utilities.GraphUtilities._

import java.nio.file.{Files, Paths}

class GraphUtilitiesTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {


  val tempDir: String = Files.createTempDirectory("tempStats").toString
  val statsFilePath: String = Paths.get(tempDir, "stats.txt").toString
  val testFilePath: String = "test.txt"

  override def afterEach(): Unit = {
    // Cleanup after each test
    Files.deleteIfExists(Paths.get(statsFilePath))
    Files.deleteIfExists(Paths.get(testFilePath))
  }

  behavior of "Utilities functions"

  it should "correctly identify valid S3 URLs" in {
    isS3Url("https://bucketname.s3.amazonaws.com/key") should be (true)
    isS3Url("https://randomurl.com/key") should be (false)
  }

  it should "replace tabs with spaces" in {
    val inputStream = new ByteArrayInputStream("\tHello\tWorld".getBytes(StandardCharsets.UTF_8))
    val outputFilePath = "test.txt"
    replaceTabsWithSpaces(inputStream, outputFilePath, 2)
    val content = scala.io.Source.fromFile(outputFilePath).mkString
    content should be ("  Hello  World")
  }


  it should "throw an exception for missing files" in {
    intercept[FileNotFoundException] {
      processYamlFile("nonexistent.yaml", "alsoNonexistent.yaml")
    }
  }

  it should "write statistics to a file correctly" in {
    val metrics = Map("metric1" -> 1.0, "metric2" -> 2.0)
    val failedAttacks = Set((1L, 2L, 0.5), (3L, 4L, 0.7))
    val successfulAttacks = Set((5L, 6L, 0.9), (7L, 8L, 1.0))

    writeStatsToFile(statsFilePath, 10, 2, 2, failedAttacks, successfulAttacks, metrics)
    val lines = scala.io.Source.fromFile(statsFilePath).getLines().toList

    lines(0) should be("Optimal Number of Walks: 10")
    lines(1) should be("Number of Successful Attacks: 2")
    lines(2) should be("Number of Failed Attacks: 2")
    lines(3) should be("")
    lines(4) should be("---- Failed Attacks ----")
    lines(5) should be("Node: 1, Valuable Node: 2, Similarity Score: 0.5")
    lines(6) should be("Node: 3, Valuable Node: 4, Similarity Score: 0.7")
    lines(7) should be("")
    lines(8) should be("---- Successful Attacks ----")
    lines(9) should be("Node: 5, Valuable Node: 6, Similarity Score: 0.9")
    lines(10) should be("Node: 7, Valuable Node: 8, Similarity Score: 1.0")
    lines(11) should be("")
    lines(12) should be("---- Metrics ----")
    lines(13) should be("metric1: 1.0")
    lines(14) should be("metric2: 2.0")
  }
}
