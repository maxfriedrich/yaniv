package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import de.maxfriedrich.yaniv.game.{Draw, DrawThrow, GameState, Throw, Yaniv}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request, Result}
import de.maxfriedrich.yaniv.game.series.{
  GameIsRunning,
  GameSeriesConfig,
  GameSeriesLogic,
  GameSeriesPreStartInfo,
  GameSeriesState,
  GameSeriesStateView,
  PlayerInfo
}
import de.maxfriedrich.yaniv.game.JsonImplicits._
import service.{GamesService, IdService}

@Singleton
class GameController @Inject() (implicit as: ActorSystem, val controllerComponents: ControllerComponents)
    extends BaseController {

  import GameController._

  implicit val ec = as.dispatcher

  val games     = new GamesService()
  val idService = new IdService()

  def newGame() = Action {
    val gameSeries = GameSeriesState.empty(GameSeriesConfig.Default, idService.nextId())
    val result = for {
      _ <- games.storage.create(gameSeries)
    } yield Ok(Json.toJson(Map("id" -> gameSeries.id)))
    resultOrError(result)
  }

  def join(gameSeriesId: String) = Action { request =>
    val result = for {
      gameSeriesState <- games.storage.getGameSeriesState(gameSeriesId)
      payload         <- requestJson[JoinGameClientResponse](request)
      info = PlayerInfo(id = idService.nextId(), name = payload.name)
      newSeriesState <- GameSeriesLogic.addPlayer(gameSeriesState, info)
      _              <- games.storage.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("id" -> info.id)))
    resultOrError(result)
  }

  def remove(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      gameSeriesState <- games.storage.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.removePlayer(gameSeriesState, playerId)
      _               <- games.storage.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def preStartInfo(gameSeriesId: String) = Action {
    val result = for {
      stateView <- games.storage.getGameSeriesPreStartInfo(gameSeriesId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def preStartInfoStream(gameSeriesId: String) = Action {
    val result = for {
      stream <- games.storage.getGameSeriesPreStartInfoStream(gameSeriesId)
    } yield sseStream[GameSeriesPreStartInfo](stream)
    resultOrError(result)
  }

  def start(gameSeriesId: String) = Action {
    val result = for {
      gameSeriesState <- games.storage.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.startSeries(gameSeriesState)
      _               <- games.storage.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def next(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      gameSeriesState <- games.storage.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.acceptGameEnding(gameSeriesState, playerId)
      _               <- games.storage.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def state(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      stateView <- games.storage.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def stateStream(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      stream <- games.storage.getGameSeriesStateStream(gameSeriesId, playerId)
    } yield sseStream[GameSeriesStateView](stream)
    resultOrError(result)
  }

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

  def gameAction(gameSeriesId: String, playerId: String, action: String) = Action { request =>
    val result = for {
      act <- action match {
        case "throw"     => requestJson[Throw](request)
        case "draw"      => requestJson[Draw](request)
        case "drawThrow" => requestJson[DrawThrow](request)
        case "yaniv"     => Right(Yaniv)
      }
      result <- games.action(gameSeriesId, playerId, act)
    } yield Ok(Json.toJson(result))
    resultOrError(result)
  }
}

object GameController {
  import play.api.mvc.Results.{BadRequest, Ok}

  def sseFormat[T: Writes](item: T): String = "data: " + Json.toJson(item).toString + "\n\n"

  def sseStream[T: Writes](stream: Source[T, _]): Result = {
    val source: Source[String, _] = stream.map(sseFormat[T])
    Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM).withHeaders("Cache-Control" -> "no-transform")
  }

  def requestJson[T: Reads](request: Request[AnyContent]): Either[String, T] = {
    val json = request.body.asJson.map(_.validate[T])
    for {
      content <- json match {
        case None                  => Left("invalid json")
        case Some(JsError(e))      => Left(s"json error: $e")
        case Some(JsSuccess(t, _)) => Right(t)
      }
    } yield content
  }

  def resultOrError(result: Either[String, Result]): Result = result match {
    case Left(err)  => BadRequest(Json.toJson(Map("error" -> err)))
    case Right(res) => res
  }
}
