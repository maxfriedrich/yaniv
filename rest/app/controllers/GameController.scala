package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request, Result}

import de.maxfriedrich.yaniv.game.{Draw, DrawThrow, Throw, Yaniv}
import de.maxfriedrich.yaniv.game.series.{AcceptNext, GameSeriesPreStartInfo, GameSeriesStateView, Join, Remove, Start}
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
    val gameSeriesId = idService.nextId()
    val result = for {
      _ <- games.createGameSeries(gameSeriesId)
    } yield Ok(Json.toJson(Map("id" -> gameSeriesId)))
    resultOrError(result)
  }

  def join(gameSeriesId: String) = Action { request =>
    val playerId = idService.nextId()
    val result = for {
      payload <- requestJson[JoinGameClientResponse](request)
      _       <- games.gameSeriesAction(gameSeriesId, Join(playerId, payload.name, isAI = false))
    } yield Ok(Json.toJson(Map("id" -> playerId)))
    resultOrError(result)
  }

  def addAI(gameSeriesId: String) = Action { request =>
    val playerId = idService.nextId()
    val result = for {
      _ <- games.gameSeriesAction(gameSeriesId, Join(playerId, "AI", isAI = true))
    } yield Ok(Json.toJson(Map("id" -> playerId)))
    resultOrError(result)
  }

  def remove(gameSeriesId: String, playerToRemove: String) = Action {
    val result = for {
      _ <- games.gameSeriesAction(gameSeriesId, Remove(playerToRemove))
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def start(gameSeriesId: String) = Action {
    val result = for {
      _ <- games.gameSeriesAction(gameSeriesId, Start)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def next(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      _ <- games.gameSeriesAction(gameSeriesId, AcceptNext(playerId))
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def preStartInfo(gameSeriesId: String) = Action {
    val result = for {
      stateView <- games.getGameSeriesPreStartInfo(gameSeriesId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def preStartInfoStream(gameSeriesId: String) = Action {
    val result = for {
      stream <- games.getGameSeriesPreStartInfoStream(gameSeriesId)
    } yield sseStream[GameSeriesPreStartInfo](stream)
    resultOrError(result)
  }

  def state(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      stateView <- games.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def stateStream(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      stream <- games.getGameSeriesStateStream(gameSeriesId, playerId)
    } yield sseStream[GameSeriesStateView](stream)
    resultOrError(result)
  }

  def gameAction(gameSeriesId: String, playerId: String, action: String) = Action { request =>
    val result = for {
      act <- action match {
        case "throw"     => requestJson[Throw](request)
        case "draw"      => requestJson[Draw](request)
        case "drawThrow" => requestJson[DrawThrow](request)
        case "yaniv"     => Right(Yaniv)
      }
      result <- games.gameAction(gameSeriesId, playerId, act)
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
