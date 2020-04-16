package service

import akka.actor.{Actor, ActorRef, Props}
import models.Game.{GameId, PlayerId}
import models.GameStateView

import scala.collection.mutable

class ConnectionManager extends Actor {
  import ConnectionManager._

  private val actors = mutable.Map.empty[(GameId, PlayerId), mutable.Set[ActorRef]]

  def receive = {
    case Update(gameId, playerId, gameState) =>
//      println("Manager got update")
      actors.getOrElse((gameId, playerId), Set.empty).foreach(_ ! gameState)
    case Register(gameId, playerId, actorRef) =>
//      println("Manager got register")
      val gamePlayerActors = actors.getOrElse((gameId, playerId), mutable.Set.empty)
      gamePlayerActors += actorRef
      actors += (gameId, playerId) -> gamePlayerActors
    case Unregister(gameId, playerId, actorRef) =>
//      println("Manager got unregister")
      val gamePlayerActors = actors.getOrElse((gameId, playerId), mutable.Set.empty)
      gamePlayerActors -= actorRef
      actors += (gameId, playerId) -> gamePlayerActors
  }
}

object ConnectionManager {
  def props: Props = Props[ConnectionManager]

  case class Update(gameId: GameId, playerId: PlayerId, gameState: GameStateView)
  case class Register(gameId: GameId, playerId: PlayerId, actorRef: ActorRef)
  case class Unregister(gameId: GameId, playerId: PlayerId, actorRef: ActorRef)
}
