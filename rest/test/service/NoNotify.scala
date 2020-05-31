package service
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContext

class NoNotify[Id, Update] extends Notify[Id, Update] {
  override def sendUpdate(id: Id, update: Update): Unit = ()
  override def signUp(id: Id)(implicit ec: ExecutionContext): Source[Update, _] = Source.empty
}
