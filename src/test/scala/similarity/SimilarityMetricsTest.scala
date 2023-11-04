package similarity
import NetGraphAlgebraDefs.NodeObject
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import similarity.SimilarityMetrics._

class SimilarityMetricsTest extends AnyFlatSpec with Matchers {
  behavior of "SimilarityMetrics functions"

  it should "return normalized attributes for given node and min-max values" in {
    val node = NodeObject(0, 1, 2, 3, 4, 5, 6, 7, 8.0, true)
    val minMaxValues = ((0, 10), (0, 10), (0, 10), (0, 10), (0, 10), (0, 10), (0, 10), (0.0, 10.0))
    val normalized = normalizeAttributes(node, minMaxValues)
    normalized shouldBe Array(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8)
  }


  it should "handle constant values in normalization" in {
    val node = NodeObject(0, 5, 5, 5, 5, 5, 5, 5, 5.0, true)
    val minMaxValues = ((5, 5), (5, 5), (5, 5), (5, 5), (5, 5), (5, 5), (5, 5), (5.0, 5.0))
    val normalized = normalizeAttributes(node, minMaxValues)
    normalized.forall(_ == 0.5) shouldBe true
  }

  it should "return correct distance between two attributes arrays" in {
    val attr1 = Array(0.1, 0.2, 0.3)
    val attr2 = Array(0.4, 0.5, 0.6)
    val distance = calculateDistance(attr1, attr2)
    val roundedDistance = BigDecimal(distance).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
    roundedDistance shouldBe 0.9
  }

  it should "return correct similarity value for two nodes" in {
    val node1 = NodeObject(1, 2, 3, 4, 5, 6, 7, 8, 0.98, true)
    val node2 = NodeObject(2, 3, 4, 5, 6, 7, 8, 9, 0.76, false)
    val minMaxValues = ((0, 10), (0, 10), (0, 10), (0, 10), (0, 10), (0, 10), (0, 10), (0.0, 10.0))
    val similarity = calculateSimilarity(node1, node2, minMaxValues)
    similarity shouldBe 1.722 // adjust as per correct expected value
  }

  it should "return correct similarity for a given distance" in {
    val distance = 2.0
    val similarity = convertDistanceToSimilarity(distance)
    similarity shouldBe 0.3333333333333333
  }

}
