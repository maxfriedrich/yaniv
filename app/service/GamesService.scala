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

  val preGameConnectionManager: ActorRef = as.actorOf(ConnectionManager.props[GameSeriesId, GameSeriesPreStartInfo])
  val inGameConnectionManager: ActorRef =
    as.actorOf(ConnectionManager.props[(GameSeriesId, PlayerId), GameSeriesStateView])

  def getGameSeriesPreStartInfo(gameSeriesId: GameSeriesId): Either[String, GameSeriesPreStartInfo] =
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) if series.nonEmpty => Right(GameSeriesPreStartInfo.fromGameSeriesState(series.last))
      case None                            => Left(s"Game series $gameSeriesId does not exist")
    }

  def getGameSeriesState(gameSeriesId: GameSeriesId): Either[String, GameSeriesState] =
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) if series.nonEmpty => Right(series.last)
      case None                            => Left(s"Game series $gameSeriesId does not exist")
    }

  def getGameSeriesStateView(
      gameSeriesId: GameSeriesId,
      playerId: PlayerId
  ): Either[String, GameSeriesStateView] =
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) if series.nonEmpty =>
        series.last.players.find(_.id == playerId) match {
          case Some(_) => Right(GameSeriesStateView.fromGameSeriesState(series.last, playerId))
          case _       => Left(s"Player $playerId is not a part of game $gameSeriesId")
        }
      case None => Left(s"Game $gameSeriesId does not exist")
    }

  def getGameSeriesStateStream(gameSeriesId: GameSeriesId, playerId: PlayerId)(
      implicit ec: ExecutionContext
  ): Either[String, Source[GameSeriesStateView, _]] =
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) if series.nonEmpty =>
        series.last.players.find(_.id == playerId) match {
          case Some(_) =>
            val source = newSourceActor(inGameConnectionManager, (gameSeriesId, playerId))
            Right(source)
          case _ => Left(s"Player $playerId is not a part of game $gameSeriesId")
        }
      case None => Left(s"Game $gameSeriesId does not exist")
    }

  def getGameSeriesPreStartInfoStream(
      gameSeriesId: GameSeriesId
  )(implicit ec: ExecutionContext): Either[String, Source[GameSeriesPreStartInfo, _]] =
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) if series.nonEmpty =>
        series.last.state match {
          case WaitingForSeriesStart => Right(newSourceActor(preGameConnectionManager, gameSeriesId))
          case _                     => Left(s"Game $gameSeriesId has already started")

        }
      case None => Left(s"Game $gameSeriesId does not exist")
    }

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
