package models.series

import models.{GameSeriesId, GameState, PlayerId}

case class PlayerInfo(id: PlayerId, name: String)

sealed trait NoCurrentGame
case object WaitingForSeriesStart                             extends NoCurrentGame
case class WaitingForNextGame(acceptedPlayers: Set[PlayerId]) extends NoCurrentGame
// TODO: Game over still needs info about cards
case class GameOver(winner: PlayerId) extends NoCurrentGame

case class GameSeriesState(
    config: GameSeriesConfig,
    id: GameSeriesId,
    version: Int,
    players: Seq[PlayerInfo],
    currentGame: Either[NoCurrentGame, GameState],
    scores: Map[PlayerId, Int],
    scoresDiff: Map[PlayerId, Int]
) {
  val timestamp: String = java.time.LocalDateTime.now.toString

  // copy forcing a version update but dumb otherwise (use GameSeriesLogic methods instead!)
  private[series] def copy(
      config: GameSeriesConfig = config,
      players: Seq[PlayerInfo] = players,
      state: Either[NoCurrentGame, GameState] = currentGame,
      scores: Map[PlayerId, Int] = scores,
      scoresDiff: Map[PlayerId, Int] = scoresDiff
  ): GameSeriesState =
    GameSeriesState(
      config = config,
      id = id,
      version = version + 1,
      players = players,
      currentGame = state,
      scores = scores,
      scoresDiff = scoresDiff
    )

}

object GameSeriesState {
  def empty(id: GameSeriesId): GameSeriesState =
    GameSeriesState(GameSeriesConfig.Default, id, 1, Seq.empty, Left(WaitingForSeriesStart), Map.empty, Map.empty)
}
