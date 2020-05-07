package models.series

import java.time.LocalDateTime

import models.{GameSeriesId, GameStateView, PlayerId}

case class GameSeriesStateView(
    config: GameSeriesConfig,
    id: GameSeriesId,
    version: Int,
    timestamp: LocalDateTime,
    me: PlayerId,
    players: Seq[PlayerInfo],
    state: HighLevelState,
    currentGame: Option[GameStateView],
    scores: Map[PlayerId, Int],
    scoresDiff: Map[PlayerId, Seq[Int]]
)

object GameSeriesStateView {
  def fromGameSeriesState(gameSeries: GameSeriesState, playerId: PlayerId): GameSeriesStateView = {
    GameSeriesStateView(
      config = gameSeries.config,
      id = gameSeries.id,
      version = gameSeries.version,
      timestamp = gameSeries.timestamp,
      me = playerId,
      players = gameSeries.players,
      state = gameSeries.state,
      currentGame = gameSeries.currentGame.map(gs => GameStateView.fromGameState(gs, playerId)),
      scores = gameSeries.scores,
      scoresDiff = gameSeries.scoresDiff
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
