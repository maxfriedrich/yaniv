package de.maxfriedrich.yaniv.ai.ml

trait Transformation {
  def apply(value: Double): Double
}

trait Invertible {
  def inverse(value: Double): Double
}

case object Identity extends Transformation with Invertible {
  def apply(value: Double): Double   = value
  def inverse(value: Double): Double = value
}

case class Inverse(invertible: Invertible) extends Transformation {
  def apply(value: Double): Double = invertible.inverse(value)
}

trait Scaler extends Transformation with Invertible {
  def apply(value: Double): Double
  def inverse(value: Double): Double
}

case class Chain(transformations: Transformation*) extends Transformation {
  def apply(value: Double): Double = transformations.foldLeft(value) { case (v, t) => t(v) }
}

case class MinMaxScaler(min: Double, max: Double = 0.0) extends Scaler {
  val range: Double                  = max - min
  def apply(value: Double): Double   = (value - min) / range
  def inverse(value: Double): Double = value * range + min
}

case class Weight(weight: Double) extends Transformation with Invertible {
  def apply(value: Double): Double   = weight * value
  def inverse(value: Double): Double = value / weight
}
