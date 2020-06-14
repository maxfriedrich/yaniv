package de.maxfriedrich.yaniv.ai.ml

import org.scalatest.flatspec.AnyFlatSpec

class PredictorSpec extends AnyFlatSpec {
  "Linear model" should "make the correct prediction" in {

    case class TestFeatures(a: Double, b: Double, c: Double)
    val model = new Predictor[TestFeatures] {
      override def featureTransformations: Seq[(Transformation, TestFeatures => Double)] =
        Seq(
          (Chain(MinMaxScaler(0.0, 10.0), Weight(25.0)), in => in.a),
          (Weight(3.0), in => in.b),
          (Identity, in => in.c),
        )

      override def aggregate(values: Seq[Double]): Double = values.sum / 25
      override def targetTransformation: Transformation = Weight(5)
    }
    assert(model(TestFeatures(8.0, 2.0, 3.0)) == ((0.8 * 25 + 6.0 + 3.0) / 25) * 5)
  }
}