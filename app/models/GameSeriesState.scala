package models

case class PlayerInfo(id: PlayerId, name: String)

case class GameSeriesState(
    id: GameSeriesId,
    version: Int,
    players: Seq[PlayerInfo],
    gameState: Option[GameState],
    scores: Map[PlayerId, Int]
)

object GameSeriesState {
  def empty(id: GameSeriesId): GameSeriesState = GameSeriesState(id, 1, Seq.empty, None, Map.empty)

  def addPlayer(gss: GameSeriesState, playerInfo: PlayerInfo): Either[String, GameSeriesState] = {
    for {
      _ <- if (gss.gameState.isDefined) Left("Game already started") else Right(())
    } yield gss.copy(
      version = gss.version + 1,
      players = gss.players :+ playerInfo,
      scores = gss.scores ++ Map(playerInfo.id -> 0)
    )
  }
}
