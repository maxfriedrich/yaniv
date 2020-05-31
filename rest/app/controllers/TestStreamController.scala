package controllers

import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.mvc.{BaseController, ControllerComponents}

@Singleton
class TestStreamController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  def testStream() = Action {
    import java.time.ZonedDateTime
    import java.time.format.DateTimeFormatter
    import scala.concurrent.duration._
    import scala.language.postfixOps

    val df: DateTimeFormatter = DateTimeFormatter.ofPattern("HH mm ss")
    val tickSource            = Source.tick(0 millis, 500 millis, "TICK")
    val source                = tickSource.map(_ => "data:" + df.format(ZonedDateTime.now()) + "\n\n")
    Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM).withHeaders("Cache-Control" -> "no-transform")
  }
}
