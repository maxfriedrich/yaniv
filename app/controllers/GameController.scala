package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import models.GameLogic
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.mvc.{BaseController, ControllerComponents, Result}
import models.JsonImplicits._
import service.{GamesService, IdService}

@Singleton
class GameController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  implicit val as = ActorSystem("yaniv")
  implicit val ec = as.dispatcher

  val gamesService = new GamesService()
  val idService    = new IdService()

  def game(gameSeriesId: String) = Action {
    Ok(views.html.game.render())
  }

  def testUpdate(gameSeriesId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
    } yield {
      gamesService.update(gameSeriesId, gameSeriesState)
      Ok(Json.toJson("ok"))
    }
    resultOrError(result)
  }

  def state(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      stateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def stream(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      stream <- gamesService.getGameSeriesStateStream(gameSeriesId, playerId)
    } yield {
      val source: Source[String, _] = stream.map { stateView =>
        "data: " + Json.toJsObject(stateView).toString + "\n\n"
      }
      Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
    }
    resultOrError(result)
  }

  def throwCards(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      json = request.body.asJson.map(_.validate[ThrowCardsClientResponse])
      cards <- json match {
        case None                                                => Left("invalid json")
        case Some(JsError(e))                                    => Left("json error: $e")
        case Some(JsSuccess(ThrowCardsClientResponse(cards), _)) => Right(cards)
      }
      newGameState      <- GameLogic.throwCards(gameSeriesState.gameState, playerId, cards)
      _             <- gamesService.update(gameSeriesId, gameSeriesState.copy(gameState = newGameState))
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def draw(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      json = request.body.asJson.map(_.validate[DrawCardClientResponse])
      source <- json match {
        case None                                               => Left("invalid json")
        case Some(JsError(e))                                   => Left("json error: $e")
        case Some(JsSuccess(DrawCardClientResponse(source), _)) => Right(source)
      }
      newGameState      <- GameLogic.drawCard(gameSeriesState.gameState, playerId, source)
      _             <- gamesService.update(gameSeriesId, gameSeriesState.copy(gameState = newGameState))
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def drawThrow(gameSeriesId: String, playerId: String) = Action { request => Ok(Json.toJson(1)) }

  def yaniv(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState     <- gamesService.getGameSeriesState(gameSeriesId)
      newGameState      <- GameLogic.callYaniv(gameSeriesState.gameState, playerId)
      _             <- gamesService.update(gameSeriesId, gameSeriesState.copy(gameState = newGameState))
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def resultOrError(result: Either[String, Result]): Result = result match {
    case Left(err)  => BadRequest(Json.toJson(Map("error" -> err)))
    case Right(res) => res
  }
}
