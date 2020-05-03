package models.series

import java.time.LocalDateTime

import models.{GameSeriesId, GameState, PlayerId}

case class PlayerInfo(id: PlayerId, name: String)

sealed trait HighLevelState
case object GameIsRunning                                     extends HighLevelState
case object WaitingForSeriesStart                             extends HighLevelState
case class WaitingForNextGame(acceptedPlayers: Set[PlayerId]) extends HighLevelState
case class GameOver(winner: PlayerId)                         extends HighLevelState

case class GameSeriesState(
    config: GameSeriesConfig,
    id: GameSeriesId,
    version: Int,
    players: Seq[PlayerInfo],
    state: HighLevelState,
    currentGame: Option[GameState],
    scores: Map[PlayerId, Int],
    scoresDiff: Map[PlayerId, Int]
) {
  val timestamp: LocalDateTime = java.time.LocalDateTime.now

  // copy forcing a version update but dumb otherwise (use GameSeriesLogic methods instead!)
  private[series] def copy(
      config: GameSeriesConfig = config,
      players: Seq[PlayerInfo] = players,
      state: HighLevelState = state,
      currentGame: Option[GameState] = currentGame,
      scores: Map[PlayerId, Int] = scores,
      scoresDiff: Map[PlayerId, Int] = scoresDiff
  ): GameSeriesState =
    GameSeriesState(
      config = config,
      id = id,
      version = version + 1,
      players = players,
      state = state,
      currentGame = currentGame,
      scores = scores,
      scoresDiff = scoresDiff
    )
}

object GameSeriesState {
  def empty(config: GameSeriesConfig, id: GameSeriesId): GameSeriesState =
    GameSeriesState(config, id, 1, Seq.empty, WaitingForSeriesStart, None, Map.empty, Map.empty)
}
