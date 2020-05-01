package models.series

import models.GameConfig

case class GameSeriesConfig(gameConfig: GameConfig, losingPoints: Int, pointRules: Seq[PointRule])

object GameSeriesConfig {
  val Default: GameSeriesConfig =
    GameSeriesConfig(GameConfig.Default, losingPoints = 100, pointRules = PointRules.Rules)
}
