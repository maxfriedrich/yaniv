package de.maxfriedrich.yaniv.ai.ml

trait Predictor[F] {
  val intercept: Double = 0.0
  def featureTransformations: Seq[(Transformation, F => Double)]
  def targetTransformation: Transformation   = Identity
  def aggregate(values: Seq[Double]): Double = values.sum

  def apply(input: F): Double = {
    val weights = featureTransformations.map { case (feature, transformInput) => feature(transformInput(input)) }
    println(s"transformed: $weights")
    val rawPred = intercept + aggregate(weights)
    println(s"raw: $rawPred")
    targetTransformation(rawPred)
  }
}
