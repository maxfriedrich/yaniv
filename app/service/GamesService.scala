package service

import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import models.Game.{GameId, PlayerId}
import models.{Cards, GameState, GameStateView, Pile, Player, Throw}

import scala.collection.mutable

class GamesService(implicit as: ActorSystem, mat: Materializer) {

  import GamesService._

  val gameStates = mutable.Map.empty[GameId, mutable.Buffer[GameState]]
  gameStates += dummyGame

  val gameStateStreams =
    mutable.Map.empty[(GameId, PlayerId), mutable.Buffer[(ActorRef, Source[GameStateView, _])]]

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
          case Some(_) =>
            Right(GameStateView.fromGameState(states.last, playerId))
        }
      case None => Left(s"Game $gameId does not exist")
    }

  def getGameStateStream(gameId: GameId, playerId: PlayerId)(
      implicit mat: Materializer
  ): Either[String, Source[GameStateView, _]] =
    gameStates.get(gameId) match {
      case Some(states) if states.nonEmpty =>
        states.last.players.find(_.id == playerId) match {
          case Some(_) =>
            val (actor, source) = newSourceActor()
            val streams         = gameStateStreams.getOrElse((gameId, playerId), mutable.Buffer.empty)
            gameStateStreams += (gameId, playerId) -> (streams :+ (actor, source))
            Right(source)
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
          gameStateStreams.get(gameId, p.id).getOrElse(Seq.empty).foreach {
            case (actor, _) =>
              println(s"Sending update to ${p.id} ${actor}")
              actor ! GameStateView.fromGameState(gameState, p.id)
          }
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
        yaniv = None
      )
    )
  }

  private def newSourceActor()(
      implicit mat: Materializer
  ): (ActorRef, Source[GameStateView, _]) = {
    val source: Source[GameStateView, _] = Source.actorRef(
      completionMatcher = {
        case Done => CompletionStrategy.immediately
      },
      failureMatcher = PartialFunction.empty,
      bufferSize = 1,
      overflowStrategy = OverflowStrategy.dropHead
    )
    val (actorRef: ActorRef, eventSource) = source.preMaterialize()
    (actorRef, eventSource)
  }

//  private def newConnectionWatchActor(sourceActor: ActorRef)(implicit as: ActorSystem): ActorRef = {
//    val connectionId = UUID.randomUUID().toString
//    val watchActor = as.actorOf(ConnectionWatchActor.props(connectionId, sourceActor), connectionId)
//
//  }
}

//class ConnectionWatchActor(connectionId: String, sourceActor: ActorRef) extends Actor {
//
//  context.watch(sourceActor)
//
//  override def receive: Receive = {
//    case Terminated(source) if source == sourceActor => context.stop(self)
//
//  }
//}
//
//object ConnectionWatchActor {
//  def props(connectionId: String, sourceActor: ActorRef): Props =
//    Props[ConnectionWatchActor](new ConnectionWatchActor(connectionId, sourceActor))
//}
