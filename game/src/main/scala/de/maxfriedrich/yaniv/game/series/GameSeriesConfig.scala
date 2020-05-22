package de.maxfriedrich.yaniv.game.series

import de.maxfriedrich.yaniv.game.GameConfig

case class GameSeriesConfig(gameConfig: GameConfig, losingPoints: Int, pointRules: Seq[PointRule], drawThrowMillis: Int)

object GameSeriesConfig {
  val Default: GameSeriesConfig =
    GameSeriesConfig(GameConfig.Default, losingPoints = 100, pointRules = PointRules.Rules, drawThrowMillis = 5000)
}
