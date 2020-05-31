package service

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import de.maxfriedrich.yaniv.game.{GameSeriesId, PlayerId}
import de.maxfriedrich.yaniv.game.series.{GameSeriesPreStartInfo, GameSeriesState, GameSeriesStateView}
import service.ConnectionManager.{Register, Unregister, Update}

import scala.concurrent.ExecutionContext

trait Notify[Id, Update] {
  def sendUpdate(id: Id, update: Update): Unit
  def signUp(id: Id)(implicit ec: ExecutionContext): Source[Update, _]
}

class NotifyWithConnectionManager[Id, Update](implicit as: ActorSystem, mat: Materializer) extends Notify[Id, Update] {

  import NotifyWithConnectionManager._
  private val cm: ActorRef = as.actorOf(ConnectionManager.props[Id, Update])

  override def sendUpdate(id: Id, update: Update): Unit                         = cm ! Update(id, update)
  override def signUp(id: Id)(implicit ec: ExecutionContext): Source[Update, _] = newSourceActor(cm, id)
}

object NotifyWithConnectionManager {
  def newSourceActor[K, V](connectionManager: ActorRef, key: K)(
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

class NotifyPreGame(implicit as: ActorSystem, mat: Materializer)
    extends NotifyWithConnectionManager[GameSeriesId, GameSeriesPreStartInfo]

class NotifyInGame(implicit as: ActorSystem, mat: Materializer)
    extends NotifyWithConnectionManager[(GameSeriesId, PlayerId), GameSeriesStateView]
