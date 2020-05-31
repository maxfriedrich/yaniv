package service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import de.maxfriedrich.yaniv.game.series._
import de.maxfriedrich.yaniv.game._

import scala.concurrent.ExecutionContext

class GamesService(implicit as: ActorSystem, mat: Materializer) {
  import GamesService._

  val storage = new GamesStorageService()

  def createGameSeries(
      gameSeriesId: GameSeriesId,
      config: GameSeriesConfig = GameSeriesConfig.Default
  ): Either[String, Unit] =
    for {
      _ <- storage.create(GameSeriesState.empty(config, gameSeriesId))
    } yield ()

  def gameSeriesAction(gameSeriesId: GameSeriesId, action: GameSeriesAction): Either[String, Unit] =
    for {
      gameSeriesState <- storage.getGameSeriesState(gameSeriesId)
      newSeriesState  <- performGameSeriesAction(gameSeriesState, action)
      _               <- storage.update(gameSeriesId, newSeriesState)
    } yield ()

  def gameAction(
      gameSeriesId: GameSeriesId,
      playerId: PlayerId,
      action: GameAction
  ): Either[String, GameSeriesStateView] =
    for {
      gameSeriesState <- storage.getGameSeriesState(gameSeriesId)
      gameState       <- getGameState(gameSeriesState)
      newGameState    <- performGameAction(gameSeriesState, gameState, playerId, action)
      newSeriesState  <- GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _               <- storage.update(gameSeriesId, newSeriesState)
      gameStateView   <- storage.getGameSeriesStateView(gameSeriesId, playerId)
    } yield gameStateView

  // TODO: not very nice to forward these, maybe it should be a mixin trait
  def getGameSeriesPreStartInfo(gameSeriesId: GameSeriesId): Either[String, GameSeriesPreStartInfo] =
    storage.getGameSeriesPreStartInfo(gameSeriesId)

  def getGameSeriesState(gameSeriesId: GameSeriesId): Either[String, GameSeriesState] =
    storage.getGameSeriesState(gameSeriesId)

  def getGameSeriesStateView(gameSeriesId: GameSeriesId, playerId: PlayerId): Either[String, GameSeriesStateView] =
    storage.getGameSeriesStateView(gameSeriesId, playerId)

  def getGameSeriesStateStream(gameSeriesId: GameSeriesId, playerId: PlayerId)(
      implicit ec: ExecutionContext
  ): Either[String, Source[GameSeriesStateView, _]] =
    storage.getGameSeriesStateStream(gameSeriesId, playerId)

  def getGameSeriesPreStartInfoStream(
      gameSeriesId: GameSeriesId
  )(implicit ec: ExecutionContext): Either[String, Source[GameSeriesPreStartInfo, _]] =
    storage.getGameSeriesPreStartInfoStream(gameSeriesId)
}

object GamesService {
  def getGameState(gameSeriesState: GameSeriesState): Either[String, GameState] = {
    (gameSeriesState.state, gameSeriesState.currentGame) match {
      case (GameIsRunning, Some(gs)) => Right(gs)
      case (noCurrentGame, _)        => Left(s"There is no current game: ${noCurrentGame.toString}")
    }
  }

  def performGameAction(
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

  def performGameSeriesAction(
      gameSeriesState: GameSeriesState,
      action: GameSeriesAction
  ): Either[String, GameSeriesState] = action match {
    case j: Join       => GameSeriesLogic.addPlayer(gameSeriesState, PlayerInfo(j.playerId, j.name))
    case r: Remove     => GameSeriesLogic.removePlayer(gameSeriesState, r.playerToRemove)
    case Start         => GameSeriesLogic.startSeries(gameSeriesState)
    case a: AcceptNext => GameSeriesLogic.acceptGameEnding(gameSeriesState, a.playerId)
  }
}
