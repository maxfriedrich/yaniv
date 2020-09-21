package de.maxfriedrich.yaniv.ai

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class PointsPredictorSpec extends AnyFlatSpec {
  "Points predictor" should "make a correct prediction" in {
    val parameters = PointsPredictorParameters(
      intercept = 0.25,
      logTurn = ScaleAndWeight(0.0, 2.5, -0.35),
      numCards = ScaleAndWeight(0.0, 5.0, 0.3)
    )
    val predictor = PointsPredictor(parameters)
    predictor(PointsPredictorFeatures(logTurn = math.log(3), numCards = 5)) should equal(19.8 +- 0.1)
  }
}
