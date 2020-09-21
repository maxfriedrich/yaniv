package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.ai.ml.{Chain, Inverse, Predictor, MinMaxScaler, Weight}
import de.maxfriedrich.yaniv.game.{GameStateView, PlayerId}

case class PointsPredictorFeatures(logTurn: Double, numCards: Int)

object PointsPredictorFeatures {
  def apply(playerId: PlayerId, gameStateView: GameStateView): PointsPredictorFeatures = {
    val player = gameStateView.otherPlayers.find(_.id == playerId)
    PointsPredictorFeatures(math.log(gameStateView.turn.toDouble), player.fold(5)(_.numCards))
  }
}

case class ScaleAndWeight(scaleMin: Double, scaleMax: Double, weight: Double)
case class PointsPredictorParameters(intercept: Double, logTurn: ScaleAndWeight, numCards: ScaleAndWeight)

case class PointsPredictor(param: PointsPredictorParameters) extends Predictor[PointsPredictorFeatures] {
  override val intercept = param.intercept
  override val featureTransformations = Seq(
    (
      Chain(MinMaxScaler(param.logTurn.scaleMin, param.logTurn.scaleMax), Weight(param.logTurn.weight)),
      in => in.logTurn
    ),
    (
      Chain(MinMaxScaler(param.numCards.scaleMin, param.numCards.scaleMax), Weight(param.numCards.weight)),
      in => in.numCards
    )
  )

  override val targetTransformation = Inverse(MinMaxScaler(0.0, 50.0))
}

object PointsPredictor {
  val DefaultWeights: PointsPredictorParameters = PointsPredictorParameters(
    intercept = 0.2646927,
    logTurn = ScaleAndWeight(0.0, 2.7080502, -0.356973),
    numCards = ScaleAndWeight(0.0, 5.0, 0.308787)
  )
  def apply(): PointsPredictor = new PointsPredictor(DefaultWeights)
}
