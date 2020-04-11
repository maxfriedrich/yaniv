package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import models.{Card, DrawSource}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.Json
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.{BaseController, ControllerComponents}
import models.GameJson._
import service.{GamesService, IdService}

@Singleton
class GameController @Inject() (val controllerComponents: ControllerComponents)
    extends BaseController {

  implicit val as = ActorSystem("yaniv")
  implicit val ec = as.dispatcher

  val gamesService = new GamesService()
  val idService = new IdService()

  def game(gameId: String) = Action {
    Ok(views.html.game.render())
  }

  def state(gameId: String, playerId: String) = Action { request =>
    gamesService.getGameStateView(gameId, playerId) match {
      case Left(err)        => BadRequest(Json.toJson(Map("error" -> err)))
      case Right(stateView) => Ok(Json.toJson(stateView))
    }
  }

  def stream(gameId: String, playerId: String) = Action { request =>
    gamesService.getGameStateStream(gameId, playerId) match {
      case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
      case Right(gsvSource) =>
        val source: Source[String, _] = gsvSource.map { stateView =>
          "data: " + Json.toJsObject(stateView).toString + "\n\n"
        }
        Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
    }
  }

  def throwCards(gameId: String, playerId: String) = Action { request =>
    gamesService.getGameState(gameId) match {
      case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
      case Right(state) => {

        request.body.asJson
          .map { json => (json \ "cards").validate[Seq[Card]] } match {
          case None => BadRequest(Json.toJson(Map("error" -> "invalid json")))
          case Some(JsError(err)) =>
            BadRequest(Json.toJson(Map("error" -> err.toString)))
          case Some(JsSuccess(cards, _)) =>
            state.throwCards(playerId, cards) match {
              case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
              case Right(newState) =>
                gamesService.update(gameId, newState) match {
                  case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
                  case Right(_) =>
                    gamesService.getGameStateView(gameId, playerId) match {
                      case Left(err)        => BadRequest(Json.toJson(Map("error" -> err)))
                      case Right(stateView) => Ok(Json.toJson(stateView))
                    }
                }
            }
        }
      }
    }
  }

  def draw(gameId: String, playerId: String) = Action { request =>
    gamesService.getGameState(gameId) match {
      case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
      case Right(state) =>
        request.body.asJson.map { json =>
          (json \ "source").validate[DrawSource]
        } match {
          case None => BadRequest(Json.toJson(Map("error" -> "invalid json")))
          case Some(JsError(err)) =>
            BadRequest(Json.toJson(Map("error" -> err.toString)))
          case Some(JsSuccess(source, _)) =>
            state.drawCard(playerId, source) match {
              case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
              case Right(newState) =>
                gamesService.update(gameId, newState) match {
                  case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
                  case Right(_) =>
                    gamesService.getGameStateView(gameId, playerId) match {
                      case Left(err)        => BadRequest(Json.toJson(Map("error" -> err)))
                      case Right(stateView) => Ok(Json.toJson(stateView))
                    }
                }
            }
        }
    }
  }

  def drawThrow(gameId: String, playerId: String) = Action { request =>
    Ok(Json.toJson(1))
  }

  def yaniv(gameId: String, playerId: String) = Action { request =>
    gamesService.getGameState(gameId) match {
      case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
      case Right(state) =>
        state.callYaniv(playerId) match {
          case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
          case Right(newState) =>
            gamesService.update(gameId, newState) match {
              case Left(err) => BadRequest(Json.toJson(Map("error" -> err)))
              case Right(_) =>
                gamesService.getGameStateView(gameId, playerId) match {
                  case Left(err)        => BadRequest(Json.toJson(Map("error" -> err)))
                  case Right(stateView) => Ok(Json.toJson(stateView))
                }
            }
        }
    }
  }
}
