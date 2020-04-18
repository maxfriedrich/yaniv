package service

import akka.actor.{Actor, ActorRef, Props}
import models.{GameSeriesId, GameSeriesStateView, PlayerId}

// TODO: this could probably be generic (K = (GameSeriesId, PlayerId), V = GameSeriesStateView
class ConnectionManager extends Actor {
  import ConnectionManager._

  private val actors = Map.empty[(GameSeriesId, PlayerId), Set[ActorRef]].withDefault(_ => Set.empty[ActorRef])

  def receive = onMessage(actors)

  private def onMessage(actors: Map[(GameSeriesId, PlayerId), Set[ActorRef]]): Receive = {
    case Update(gameId, playerId, gameState) =>
      //      println("Manager got update")
      actors.getOrElse((gameId, playerId), Set.empty).foreach(_ ! gameState)
    case Register(gameId, playerId, actorRef) =>
      //      println("Manager got register")
      val newGamePlayerActors = actors(gameId, playerId) + actorRef
      context.become(onMessage(actors + ((gameId, playerId) -> newGamePlayerActors)))
    case Unregister(gameId, playerId, actorRef) =>
      //      println("Manager got unregister")
      val newGamePlayerActors = actors(gameId, playerId) - actorRef
      context.become(onMessage(actors + ((gameId, playerId) -> newGamePlayerActors)))
  }
}

object ConnectionManager {
  def props: Props = Props[ConnectionManager]

  case class Update(gameId: GameSeriesId, playerId: PlayerId, gameSeriesStateView: GameSeriesStateView)
  case class Register(gameId: GameSeriesId, playerId: PlayerId, actorRef: ActorRef)
  case class Unregister(gameId: GameSeriesId, playerId: PlayerId, actorRef: ActorRef)
}
