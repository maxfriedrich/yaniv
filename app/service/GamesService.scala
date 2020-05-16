package service

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import models.series.{GameSeriesPreStartInfo, GameSeriesState, GameSeriesStateView, WaitingForSeriesStart}
import models.{DummyGame, GameSeriesId, PlayerId}
import service.ConnectionManager.{Register, Unregister, Update}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class GamesService(implicit as: ActorSystem, mat: Materializer) {

  import GamesService._

  val gameSeriesStates = mutable.Map.empty[GameSeriesId, mutable.Buffer[GameSeriesState]]
  gameSeriesStates += DummyGame.dummyGame
  gameSeriesStates += DummyGame.drawThrowTest
  gameSeriesStates += DummyGame.betweenGames
  gameSeriesStates += DummyGame.fiveCardsOnPile
  gameSeriesStates += DummyGame.gameOver
  gameSeriesStates += DummyGame.singleCards

  val secrets = mutable.Map.empty[(GameSeriesId, PlayerId), String]
  gameSeriesStates.foreach {
    case (gameSeriesId, statesBuffer) =>
      statesBuffer.last.players.foreach { player => secrets += (gameSeriesId, player.id) -> "secret" }
  }

  val preGameConnectionManager: ActorRef = as.actorOf(ConnectionManager.props[GameSeriesId, GameSeriesPreStartInfo])
  val inGameConnectionManager: ActorRef =
    as.actorOf(ConnectionManager.props[(GameSeriesId, PlayerId), GameSeriesStateView])

  def getGameSeriesPreStartInfo(gameSeriesId: GameSeriesId): Either[String, GameSeriesPreStartInfo] =
    for (state <- validateGame(gameSeriesId)) yield GameSeriesPreStartInfo.fromGameSeriesState(state)

  def getGameSeriesState(gameSeriesId: GameSeriesId): Either[String, GameSeriesState] =
    for (state <- validateGame(gameSeriesId)) yield state

  def getGameSeriesState(
      gameSeriesId: GameSeriesId,
      playerId: PlayerId,
      secret: String
  ): Either[String, GameSeriesState] =
    for (state <- validateGamePlayerSecret(gameSeriesId, playerId, secret)) yield state

  def getGameSeriesStateView(
      gameSeriesId: GameSeriesId,
      playerId: PlayerId,
      secret: String
  ): Either[String, GameSeriesStateView] =
    for {
      gameState <- validateGamePlayerSecret(gameSeriesId, playerId, secret)
    } yield GameSeriesStateView.fromGameSeriesState(gameState, playerId)

  def getGameSeriesStateStream(gameSeriesId: GameSeriesId, playerId: PlayerId, secret: String)(
      implicit ec: ExecutionContext
  ): Either[String, Source[GameSeriesStateView, _]] =
    for {
      _ <- validateGamePlayerSecret(gameSeriesId, playerId, secret)
    } yield newSourceActor(inGameConnectionManager, (gameSeriesId, playerId))

  def getGameSeriesPreStartInfoStream(
      gameSeriesId: GameSeriesId
  )(implicit ec: ExecutionContext): Either[String, Source[GameSeriesPreStartInfo, _]] =
    for {
      state <- validateGame(gameSeriesId)
      _     <- Either.cond(state.state == WaitingForSeriesStart, (), s"Game $gameSeriesId has already started")
    } yield newSourceActor(preGameConnectionManager, gameSeriesId)

  def create(initialState: GameSeriesState): Either[String, String] = {
    val gameSeriesId = initialState.id
    if (gameSeriesStates.contains(gameSeriesId))
      Left(s"Game $gameSeriesId already exists")
    else {
      gameSeriesStates += gameSeriesId -> mutable.Buffer(initialState)
      Right("ok")
    }
  }

  def update(gameSeriesId: GameSeriesId, gameSeriesState: GameSeriesState): Either[String, String] = {
    // TODO this could also use the for/yield validations
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) =>
        gameSeriesStates += gameSeriesId -> (series :+ gameSeriesState)
        println("Sending pre-game start info update")
        preGameConnectionManager ! Update(gameSeriesId, GameSeriesPreStartInfo.fromGameSeriesState(gameSeriesState))
        gameSeriesState.players.foreach { p =>
          println(s"Sending update to ${p.id}")
          inGameConnectionManager ! Update(
            (gameSeriesId, p.id),
            GameSeriesStateView.fromGameSeriesState(gameSeriesState, p.id)
          )
        }
        Right("ok")
      case None => Left(s"Game $gameSeriesId does not exist")
    }
  }

  def storeSecret(gameSeriesId: GameSeriesId, playerId: PlayerId, secret: String): Either[String, Unit] = {
    for {
      _ <- validateGame(gameSeriesId)
    } yield {
      secrets += (gameSeriesId, playerId) -> secret
      ()
    }
  }

  private def validateGame(gameSeriesId: GameSeriesId): Either[String, GameSeriesState] =
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) if series.nonEmpty => Right(series.last)
      case _                               => Left(s"Game series $gameSeriesId does not exist")
    }

  private def validateGamePlayerSecret(
      gameSeriesId: GameSeriesId,
      playerId: PlayerId,
      secret: String
  ): Either[String, GameSeriesState] = {
    for {
      state <- validateGame(gameSeriesId)
      _ <- Either.cond(
        state.players.exists(_.id == playerId),
        (),
        s"Player $playerId is not a part of game $gameSeriesId"
      )
      _ <- Either.cond(
        secrets.get(gameSeriesId, playerId).fold(false)(_ == secret),
        (),
        s"Secret for player $playerId in game $gameSeriesId is not valid"
      )
    } yield state
  }
}

object GamesService {
  private def newSourceActor[K, V](connectionManager: ActorRef, key: K)(
      implicit ec: ExecutionContext
  ): Source[V, ActorRef] = {
    Source
      .actorRef(
        completionMatcher = {
          case Done => CompletionStrategy.immediately
        },
        failureMatcher = PartialFunction.empty,
        bufferSize = 32,
        overflowStrategy = OverflowStrategy.dropHead
      )
      .watchTermination() {
        case (actorRef: ActorRef, terminate) =>
          connectionManager ! Register(key, actorRef)
          terminate.onComplete(_ => connectionManager ! Unregister(key, actorRef))
          actorRef
      }
  }
}
