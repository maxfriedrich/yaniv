package service

import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import models.Game.{GameId, PlayerId}
import models.{Cards, GameState, GameStateView, Pile, Player, Throw}
import service.ConnectionManager.{Register, Unregister, Update}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class GamesService(implicit as: ActorSystem, mat: Materializer) {

  import GamesService._

  val gameStates = mutable.Map.empty[GameId, mutable.Buffer[GameState]]
  gameStates += dummyGame

  val connectionManager = as.actorOf(ConnectionManager.props)

  def getGameState(gameId: GameId): Either[String, GameState] =
    gameStates.get(gameId) match {
      case Some(states) if states.nonEmpty => Right(states.last)
      case None                            => Left(s"Game $gameId does not exist")
    }

  def getGameStateView(
      gameId: GameId,
      playerId: PlayerId
  ): Either[String, GameStateView] =
    gameStates.get(gameId) match {
      case Some(states) if states.nonEmpty =>
        states.last.players.find(_.id == playerId) match {
          case Some(_) => Right(GameStateView.fromGameState(states.last, playerId))
          case _       => Left(s"Player $playerId is not a part of game $gameId")
        }
      case None => Left(s"Game $gameId does not exist")
    }

  def getGameStateStream(gameId: GameId, playerId: PlayerId)(
      implicit ec: ExecutionContext
  ): Either[String, Source[GameStateView, _]] =
    gameStates.get(gameId) match {
      case Some(states) if states.nonEmpty =>
        states.last.players.find(_.id == playerId) match {
          case Some(_) =>
            val source = newSourceActor(connectionManager, gameId, playerId)
            Right(source)
          case _ => Left(s"Player $playerId is not a part of game $gameId")
        }
      case None => Left(s"Game $gameId does not exist")
    }

  def create(gameId: GameId, gameState: GameState): Either[String, String] = {
    gameStates += gameId -> mutable.Buffer(gameState)
    Right("ok")
  }

  def update(gameId: GameId, gameState: GameState): Either[String, String] = {
    gameStates.get(gameId) match {
      case Some(states) =>
        gameStates += gameId -> (states :+ gameState)
        gameState.players.foreach { p =>
          println(s"Sending update to ${p.id}")
          connectionManager ! Update(gameId, p.id, GameStateView.fromGameState(gameState, p.id))
        }
        Right("ok")
      case None => Left(s"Game $gameId does not exist")
    }
  }
}

object GamesService {
  val dummyGame = "g1" -> {
    val initialDeck = Cards.shuffledDeck()
    val pile        = Pile.newPile(initialDeck.head)
    val p1Cards     = initialDeck.drop(1).take(5)
    val p2Cards     = initialDeck.drop(6).take(5)
    val deck        = initialDeck.drop(11)

    mutable.Buffer(
      GameState(
        id = "g1",
        version = 1,
        players = Seq(Player("p1", "Max", p1Cards), Player("p2", "Pauli", p2Cards)),
        currentPlayer = "p1",
        nextAction = Throw,
        deck = deck,
        pile = pile,
        ending = None
      )
    )
  }

  private def newSourceActor(connectionManager: ActorRef, gameId: GameId, playerId: PlayerId)(
      implicit ec: ExecutionContext
  ): Source[GameStateView, ActorRef] = {
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
          connectionManager ! Register(gameId, playerId, actorRef)
          terminate.onComplete(_ => connectionManager ! Unregister(gameId, playerId, actorRef))
          actorRef
      }
  }
}
