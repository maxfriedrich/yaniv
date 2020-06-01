package service

import akka.actor.{Actor, ActorRef, Props}

class ConnectionManager[K, V] extends Actor {
  import ConnectionManager._

  private val actors = Map.empty[K, Set[ActorRef]].withDefault(_ => Set.empty[ActorRef])

  def receive: Receive = onMessage(actors)

  private def onMessage(actors: Map[K, Set[ActorRef]]): Receive = {
    case Update(key: K, value: V) =>
      actors.getOrElse(key, Set.empty).foreach(_ ! value)
    case Register(key: K, actorRef) =>
      val newGamePlayerActors = actors(key) + actorRef
      context.become(onMessage(actors + (key -> newGamePlayerActors)))
    case Unregister(key: K, actorRef) =>
      val newGamePlayerActors = actors(key) - actorRef
      context.become(onMessage(actors + (key -> newGamePlayerActors)))
  }
}

object ConnectionManager {
  def props[K, V]: Props = Props[ConnectionManager[K, V]]

  case class Update[K, V](key: K, value: V)
  case class Register[K](key: K, actorRef: ActorRef)
  case class Unregister[K](key: K, actorRef: ActorRef)
}
