package models

case class PlayerInfo(id: PlayerId, name: String)

case class GameSeriesState(
    id: GameSeriesId,
    version: Int,
    players: Seq[PlayerInfo],
    gameState: GameState,
    scores: Map[PlayerId, Int]
)
