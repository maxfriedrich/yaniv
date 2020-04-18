package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import models.{GameLogic, GameSeriesState, GameState, PlayerInfo}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request, Result}
import models.JsonImplicits._
import service.{GamesService, IdService}

@Singleton
class GameController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  import GameController._

  implicit val as = ActorSystem("yaniv")
  implicit val ec = as.dispatcher

  val gamesService = new GamesService()
  val idService    = new IdService()

  def game(gameSeriesId: String) = Action {
    Ok(views.html.game.render())
  }

  def newGame() = Action { request =>
    val gameSeries = GameSeriesState.empty(idService.nextId())
    val result = for {
      _ <- gamesService.create(gameSeries)
    } yield Ok(Json.toJson(Map("id" -> gameSeries.id)))
    resultOrError(result)
  }

  def join(gameSeriesId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      payload         <- requestJson[JoinGameClientResponse](request)
      info = PlayerInfo(id = idService.nextId(), name = payload.name)
      newSeriesState <- GameSeriesState.addPlayer(gameSeriesState, info)
      _              <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson("ok"))
    resultOrError(result)
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
      payload         <- requestJson[ThrowCardsClientResponse](request)
      cards = payload.cards
      gameState     <- getGameState(gameSeriesState)
      newGameState  <- GameLogic.throwCards(gameState, playerId, cards)
      _             <- gamesService.update(gameSeriesId, gameSeriesState.copy(gameState = Some(newGameState)))
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def draw(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      payload         <- requestJson[DrawCardClientResponse](request)
      source = payload.source
      gameState     <- getGameState(gameSeriesState)
      newGameState  <- GameLogic.drawCard(gameState, playerId, source)
      _             <- gamesService.update(gameSeriesId, gameSeriesState.copy(gameState = Some(newGameState)))
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def drawThrow(gameSeriesId: String, playerId: String) = Action { request => Ok(Json.toJson(1)) }

  def yaniv(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      gameState       <- getGameState(gameSeriesState)
      newGameState    <- GameLogic.callYaniv(gameState, playerId)
      _               <- gamesService.update(gameSeriesId, gameSeriesState.copy(gameState = Some(newGameState)))
      gameStateView   <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }
}

object GameController {
  import play.api.mvc.Results.BadRequest

  def requestJson[T: Reads](request: Request[AnyContent]): Either[String, T] = {
    val json = request.body.asJson.map(_.validate[T])
    for {
      content <- json match {
        case None                  => Left("invalid json")
        case Some(JsError(e))      => Left("json error: $e")
        case Some(JsSuccess(t, _)) => Right(t)
      }
    } yield content
  }

  def resultOrError(result: Either[String, Result]): Result = result match {
    case Left(err)  => BadRequest(Json.toJson(Map("error" -> err)))
    case Right(res) => res
  }

  def getGameState(gameSeriesState: GameSeriesState): Either[String, GameState] = {
    gameSeriesState.gameState match {
      case Some(gs) => Right(gs)
      case None     => Left("Game has not started")
    }
  }
}
