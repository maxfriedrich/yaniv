package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import models.{GameLogic, GameState}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request, Result}
import models.series.{GameSeriesLogic, GameSeriesState, PlayerInfo}
import models.JsonImplicits._
import service.{GamesService, IdService}

@Singleton
class GameController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  import GameController._

  implicit val as = ActorSystem("yaniv")
  implicit val ec = as.dispatcher

  val gamesService = new GamesService()
  val idService    = new IdService()

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
      newSeriesState <- GameSeriesLogic.addPlayer(gameSeriesState, info)
      _              <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("id" -> info.id)))
    resultOrError(result)
  }

  def preStartInfo(gameSeriesId: String) = Action { request =>
    val result = for {
      stateView <- gamesService.getGameSeriesPreStartInfo(gameSeriesId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def preStartInfoStream(gameSeriesId: String) = Action { request =>
    val result = for {
      stream <- gamesService.getGameSeriesPreStartInfoStream(gameSeriesId)
    } yield {
      val source: Source[String, _] = stream.map { info => "data: " + Json.toJson(info).toString + "\n\n" }
      Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
    }
    resultOrError(result)
  }

  def start(gameSeriesId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.startSeries(gameSeriesState)
      _               <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def testUpdate(gameSeriesId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
    } yield {
      gamesService.update(gameSeriesId, gameSeriesState)
      Ok(Json.toJson(Map("ok" -> "ok")))
    }
    resultOrError(result)
  }

  def state(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      stateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def stateStream(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      stream <- gamesService.getGameSeriesStateStream(gameSeriesId, playerId)
    } yield {
      val source: Source[String, _] = stream.map { stateView => "data: " + Json.toJson(stateView).toString + "\n\n" }
      Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
    }
    resultOrError(result)
  }

  def throwCards(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      payload         <- requestJson[ThrowCardsClientResponse](request)
      cards = payload.cards
      gameState    <- getGameState(gameSeriesState)
      newGameState <- GameLogic.throwCards(gameState, playerId, cards)
      newSeriesState = GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _             <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def draw(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      payload         <- requestJson[DrawCardClientResponse](request)
      source = payload.source
      gameState    <- getGameState(gameSeriesState)
      newGameState <- GameLogic.drawCard(gameState, playerId, source)
      newSeriesState = GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _             <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def drawThrow(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      payload         <- requestJson[DrawThrowCardClientResponse](request)
      card = payload.card
      gameState <- getGameState(gameSeriesState)
      // TODO: accept draw-throw move only if the passd time since the last game state is < some threshold
      newGameState <- GameLogic.drawThrowCard(gameState, playerId, card)
      newSeriesState = GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _             <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(gameStateView))
    resultOrError(result)
  }

  def yaniv(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      gameState       <- getGameState(gameSeriesState)
      newGameState    <- GameLogic.callYaniv(gameState, playerId)
      newSeriesState = GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _             <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
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
        case Some(JsError(e))      => Left(s"json error: $e")
        case Some(JsSuccess(t, _)) => Right(t)
      }
    } yield content
  }

  def resultOrError(result: Either[String, Result]): Result = result match {
    case Left(err)  => BadRequest(Json.toJson(Map("error" -> err)))
    case Right(res) => res
  }

  def getGameState(gameSeriesState: GameSeriesState): Either[String, GameState] = {
    gameSeriesState.currentGame match {
      case Right(gs) => Right(gs)
      case Left(ncg) => Left(s"There is no current game: ${ncg.toString}")
    }
  }
}
