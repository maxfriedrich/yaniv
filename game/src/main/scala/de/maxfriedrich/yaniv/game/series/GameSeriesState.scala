package de.maxfriedrich.yaniv.game.series

import java.time.LocalDateTime

import de.maxfriedrich.yaniv.game.{GameSeriesId, GameState, PlayerId}

case class PlayerInfo(id: PlayerId, name: String, isAI: Boolean = false)

sealed trait HighLevelState
sealed trait WaitingForNext {
  val acceptedPlayers: Set[PlayerId]
}

case object GameIsRunning                                             extends HighLevelState
case object WaitingForSeriesStart                                     extends HighLevelState
case class WaitingForNextGame(acceptedPlayers: Set[PlayerId])         extends HighLevelState with WaitingForNext
case class GameOver(winner: PlayerId, acceptedPlayers: Set[PlayerId]) extends HighLevelState with WaitingForNext

sealed trait GameSeriesAction
case class Join(playerId: PlayerId, name: String, isAI: Boolean) extends GameSeriesAction
case class Remove(playerToRemove: PlayerId)                      extends GameSeriesAction
case object Start                                                extends GameSeriesAction
case class AcceptNext(playerId: PlayerId)                        extends GameSeriesAction

case class GameSeriesState(
    config: GameSeriesConfig,
    id: GameSeriesId,
    version: Int = 0,
    players: Seq[PlayerInfo],
    state: HighLevelState,
    currentGame: Option[GameState],
    lastWinner: Option[PlayerId],
    scores: Map[PlayerId, Int],
    scoresDiff: Map[PlayerId, Seq[Int]]
) {
  val timestamp: LocalDateTime = java.time.LocalDateTime.now

  // copy forcing a version update but dumb otherwise (use GameSeriesLogic methods instead!)
  // TODO: not very nice to mix logic into the case class :/
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
