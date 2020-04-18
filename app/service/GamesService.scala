package service

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import models.{DummyGame, GameSeriesState, GameSeriesId, GameSeriesStateView, GameState, PlayerId}
import service.ConnectionManager.{Register, Unregister, Update}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class GamesService(implicit as: ActorSystem, mat: Materializer) {

  import GamesService._

  val gameSeriesStates = mutable.Map.empty[GameSeriesId, mutable.Buffer[GameSeriesState]]
  gameSeriesStates += DummyGame.dummyGame

  val connectionManager = as.actorOf(ConnectionManager.props)

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
            val source = newSourceActor(connectionManager, gameSeriesId, playerId)
            Right(source)
          case _ => Left(s"Player $playerId is not a part of game $gameSeriesId")
        }
      case None => Left(s"Game $gameSeriesId does not exist")
    }

  def create(gameSeriesId: GameSeriesId, gameSeriesState: GameSeriesState): Either[String, String] = {
    gameSeriesStates += gameSeriesId -> mutable.Buffer(gameSeriesState)
    Right("ok")
  }

  def update(gameSeriesId: GameSeriesId, gameSeriesState: GameSeriesState): Either[String, String] = {
    gameSeriesStates.get(gameSeriesId) match {
      case Some(series) =>
        gameSeriesStates += gameSeriesId -> (series :+ gameSeriesState)
        gameSeriesState.players.foreach { p =>
          println(s"Sending update to ${p.id}")
          connectionManager ! Update(gameSeriesId, p.id, GameSeriesStateView.fromGameSeriesState(gameSeriesState, p.id))
        }
        Right("ok")
      case None => Left(s"Game $gameSeriesId does not exist")
    }
  }
}

object GamesService {
  private def newSourceActor(connectionManager: ActorRef, gameSeriesId: GameSeriesId, playerId: PlayerId)(
      implicit ec: ExecutionContext
  ): Source[GameSeriesStateView, ActorRef] = {
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
          println("watch termination")
          connectionManager ! Register(gameSeriesId, playerId, actorRef)
          terminate.onComplete(_ => connectionManager ! Unregister(gameSeriesId, playerId, actorRef))
          actorRef
      }
  }
}
