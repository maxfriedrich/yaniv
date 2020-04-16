package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.mvc.{BaseController, ControllerComponents, Result}
import models.json._
import service.{GamesService, IdService}

@Singleton
class GameController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  implicit val as = ActorSystem("yaniv")
  implicit val ec = as.dispatcher

  val gamesService = new GamesService()
  val idService    = new IdService()

  def game(gameId: String) = Action {
    Ok(views.html.game.render())
  }

  def testUpdate(gameId: String) = Action { request =>
    val result = for {
      gameState <- gamesService.getGameState(gameId)
    } yield {
      gamesService.update(gameId, gameState)
      Ok(Json.toJson("ok"))
    }
    resultOrError(result)
  }

  def state(gameId: String, playerId: String) = Action { request =>
    val result = for {
      stateView <- gamesService.getGameStateView(gameId, playerId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def stream(gameId: String, playerId: String) = Action { request =>
    val result = for {
      stream <- gamesService.getGameStateStream(gameId, playerId)
    } yield {
      val source: Source[String, _] = stream.map { stateView =>
        "data: " + Json.toJsObject(stateView).toString + "\n\n"
      }
      Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
    }
    resultOrError(result)
  }

  def throwCards(gameId: String, playerId: String) = Action { request =>
    val result = for {
      gameState <- gamesService.getGameState(gameId)
      json = request.body.asJson.map(_.validate[ThrowCardsClientResponse])
      cards <- json match {
        case None                                                => Left("invalid json")
        case Some(JsError(e))                                    => Left("json error: $e")
        case Some(JsSuccess(ThrowCardsClientResponse(cards), _)) => Right(cards)
      }
      newState      <- gameState.throwCards(playerId, cards)
      _             <- gamesService.update(gameId, newState)
      gameStateView <- gamesService.getGameStateView(gameId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def draw(gameId: String, playerId: String) = Action { request =>
    val result = for {
      gameState <- gamesService.getGameState(gameId)
      json = request.body.asJson.map(_.validate[DrawCardClientResponse])
      source <- json match {
        case None                                               => Left("invalid json")
        case Some(JsError(e))                                   => Left("json error: $e")
        case Some(JsSuccess(DrawCardClientResponse(source), _)) => Right(source)
      }
      newState      <- gameState.drawCard(playerId, source)
      _             <- gamesService.update(gameId, newState)
      gameStateView <- gamesService.getGameStateView(gameId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def drawThrow(gameId: String, playerId: String) = Action { request => Ok(Json.toJson(1)) }

  def yaniv(gameId: String, playerId: String) = Action { request =>
    val result = for {
      gameState     <- gamesService.getGameState(gameId)
      newState      <- gameState.callYaniv(playerId)
      _             <- gamesService.update(gameId, newState)
      gameStateView <- gamesService.getGameStateView(gameId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def resultOrError(result: Either[String, Result]): Result = result match {
    case Left(err)  => BadRequest(Json.toJson(Map("error" -> err)))
    case Right(res) => res
  }
}
