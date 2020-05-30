package service

import akka.actor.ActorSystem
import akka.stream.Materializer
import de.maxfriedrich.yaniv.game.series.{GameIsRunning, GameSeriesLogic, GameSeriesState, GameSeriesStateView}
import de.maxfriedrich.yaniv.game.{
  Draw,
  DrawThrow,
  GameAction,
  GameLogic,
  GameSeriesId,
  GameState,
  PlayerId,
  Throw,
  Yaniv
}

class GamesService(implicit as: ActorSystem, mat: Materializer) {
  import GamesService._

  val storage = new GamesStorageService()

  def action(gameSeriesId: GameSeriesId, playerId: PlayerId, action: GameAction): Either[String, GameSeriesStateView] =
    for {
      gameSeriesState <- storage.getGameSeriesState(gameSeriesId)
      gameState       <- getGameState(gameSeriesState)
      newGameState    <- performAction(gameSeriesState, gameState, playerId, action)
      newSeriesState  <- GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _               <- storage.update(gameSeriesId, newSeriesState)
      gameStateView   <- storage.getGameSeriesStateView(gameSeriesId, playerId)
    } yield gameStateView
}

object GamesService {
  def getGameState(gameSeriesState: GameSeriesState): Either[String, GameState] = {
    (gameSeriesState.state, gameSeriesState.currentGame) match {
      case (GameIsRunning, Some(gs)) => Right(gs)
      case (noCurrentGame, _)        => Left(s"There is no current game: ${noCurrentGame.toString}")
    }
  }

  def performAction(
      gameSeriesState: GameSeriesState,
      gameState: GameState,
      playerId: PlayerId,
      action: GameAction
  ): Either[String, GameState] = action match {
    case d: Draw  => GameLogic.drawCard(gameState, playerId, d.source)
    case t: Throw => GameLogic.throwCards(gameState, playerId, t.cards)
    case dt: DrawThrow =>
      for {
        _ <- Either
          .cond(GameSeriesLogic.isDrawThrowTimingAccepted(gameSeriesState), (), "Draw-throw timing not accepted")
        s <- GameLogic.drawThrowCard(gameState, playerId, dt.card)
      } yield s
    case Yaniv => GameLogic.callYaniv(gameState, playerId)
  }
}
