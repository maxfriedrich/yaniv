package models.series

import java.time.LocalDateTime

import models.{GameSeriesId, GameState, PlayerId}

case class PlayerInfo(id: PlayerId, name: String)

sealed trait HighLevelState
sealed trait WaitingForNext {
  val acceptedPlayers: Set[PlayerId]
}

case object GameIsRunning                                             extends HighLevelState
case object WaitingForSeriesStart                                     extends HighLevelState
case class WaitingForNextGame(acceptedPlayers: Set[PlayerId])         extends HighLevelState with WaitingForNext
case class GameOver(winner: PlayerId, acceptedPlayers: Set[PlayerId]) extends HighLevelState with WaitingForNext

case class GameSeriesState(
    config: GameSeriesConfig,
    id: GameSeriesId,
    version: Int,
    players: Seq[PlayerInfo],
    state: HighLevelState,
    currentGame: Option[GameState],
    lastWinner: Option[PlayerId],
    scores: Map[PlayerId, Int],
    scoresDiff: Map[PlayerId, Seq[Int]]
) {
  val timestamp: LocalDateTime = java.time.LocalDateTime.now

  // copy forcing a version update but dumb otherwise (use GameSeriesLogic methods instead!)
  private[series] def copy(
      config: GameSeriesConfig = config,
      players: Seq[PlayerInfo] = players,
      state: HighLevelState = state,
      currentGame: Option[GameState] = currentGame,
      lastWinner: Option[PlayerId] = lastWinner,
      scores: Map[PlayerId, Int] = scores,
      scoresDiff: Map[PlayerId, Seq[Int]] = scoresDiff
  ): GameSeriesState =
    GameSeriesState(
      config = config,
      id = id,
      version = version + 1,
      players = players,
      state = state,
      currentGame = currentGame,
      lastWinner = lastWinner,
      scores = scores,
      scoresDiff = scoresDiff
    )
}

object GameSeriesState {
  def empty(config: GameSeriesConfig, id: GameSeriesId): GameSeriesState =
    GameSeriesState(config, id, 1, Seq.empty, WaitingForSeriesStart, None, None, Map.empty, Map.empty)
}
