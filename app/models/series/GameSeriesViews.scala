package models.series

import models.{GameSeriesId, GameStateView, PlayerId}

case class GameSeriesStateView(
                                config: GameSeriesConfig,
                                id: GameSeriesId,
                                version: Int,
                                timestamp: String,
                                players: Seq[PlayerInfo],
                                currentGame: Either[NoCurrentGame, GameStateView],
                                scores: Map[PlayerId, Int]
)

object GameSeriesStateView {
  def fromGameSeriesState(gameSeries: GameSeriesState, playerId: PlayerId): GameSeriesStateView = {
    GameSeriesStateView(
      config = gameSeries.config,
      id = gameSeries.id,
      version = gameSeries.version,
      timestamp = gameSeries.timestamp,
      players = gameSeries.players,
      currentGame = gameSeries.currentGame.map(gs => GameStateView.fromGameState(gs, playerId)),
      scores = gameSeries.scores
    )
  }
}

case class GameSeriesPreStartInfo(
    id: GameSeriesId,
    version: Int,
    players: Seq[PlayerInfo]
)

object GameSeriesPreStartInfo {
  def fromGameSeriesState(gameSeries: GameSeriesState): GameSeriesPreStartInfo = {
    GameSeriesPreStartInfo(gameSeries.id, gameSeries.version, gameSeries.players)
  }
}
