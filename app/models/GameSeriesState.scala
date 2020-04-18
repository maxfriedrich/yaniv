package models

case class PlayerInfo(id: PlayerId, name: String)

case class GameSeriesState(
    id: GameSeriesId,
    version: Int,
    players: Seq[PlayerInfo],
    gameState: Option[GameState],
    scores: Map[PlayerId, Int]
) {
  val timestamp: String = java.time.LocalDateTime.now.toString

  def copy(
      players: Seq[PlayerInfo] = players,
      gameState: Option[GameState] = gameState,
      scores: Map[PlayerId, Int] = scores
  ): GameSeriesState = {
    GameSeriesState(id = id, version = version + 1, players = players, gameState = gameState, scores = scores)
  }
}

object GameSeriesState {
  def empty(id: GameSeriesId): GameSeriesState = GameSeriesState(id, 1, Seq.empty, None, Map.empty)

  def addPlayer(gss: GameSeriesState, playerInfo: PlayerInfo): Either[String, GameSeriesState] = {
    for {
      _ <- if (gss.gameState.isDefined) Left("Game already started") else Right(())
    } yield gss.copy(
      players = gss.players :+ playerInfo,
      scores = gss.scores ++ Map(playerInfo.id -> 0)
    )
  }

  def start(gss: GameSeriesState): Either[String, GameSeriesState] = {
    for {
      _ <- gss.gameState match {
        case None                            => Right(())
        case Some(gs) if gs.ending.isDefined => Right(())
        case _                               => Left("There is a running game")
      }
    } yield gss.copy(
      gameState = Some(GameState.newGame(gss.players))
    )
  }
}
