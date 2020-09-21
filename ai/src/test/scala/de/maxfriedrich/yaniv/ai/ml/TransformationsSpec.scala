package de.maxfriedrich.yaniv.ai.ml

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class TransformationsSpec extends AnyFlatSpec {

  "Scaler" should "transform correctly" in {

    assert(MinMaxScaler(0.0, 2.0)(1.0) == 0.5)
    assert(MinMaxScaler(0.0, 2.0).inverse(0.5) == 1.0)

    assert(MinMaxScaler(1.0, 2.0)(1) == 0.0)
    assert(MinMaxScaler(1.0, 2.0).inverse(0.0) == 1.0)
  }

  "Transformations chain" should "apply multiple transformations in the correct order" in {
    assert(Chain(MinMaxScaler(0.0, 2.0), Weight(10.0))(1.0) == 5.0)
    assert(
      Chain(MinMaxScaler(0.0, 2.0), Identity, Weight(10.0), Identity, MinMaxScaler(2.5, 12.5))(1.0) == 0.25
    )
  }

}
