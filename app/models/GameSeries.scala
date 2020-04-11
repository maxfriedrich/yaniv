package models

import models.Game.PlayerId

case class GameSeries(
    players: Seq[Player],
    scores: Map[PlayerId, Int],
    currentGame: GameState
)
