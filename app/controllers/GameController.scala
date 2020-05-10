package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import models.{GameLogic, GameState}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request, Result}
import models.series.{
  GameIsRunning,
  GameSeriesConfig,
  GameSeriesLogic,
  GameSeriesPreStartInfo,
  GameSeriesState,
  GameSeriesStateView,
  PlayerInfo
}
import models.JsonImplicits._
import service.{GamesService, IdService}

@Singleton
class GameController @Inject() (implicit as: ActorSystem, val controllerComponents: ControllerComponents)
    extends BaseController {

  import GameController._

  implicit val ec = as.dispatcher

  val gamesService = new GamesService()
  val idService    = new IdService()

  def newGame() = Action { request =>
    val gameSeries = GameSeriesState.empty(GameSeriesConfig.Default, idService.nextId())
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

  def remove(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.removePlayer(gameSeriesState, playerId)
      _               <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
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
    } yield sseStream[GameSeriesPreStartInfo](stream)
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

  def next(gameSeriesId: String, playerId: String) = Action { request =>
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.acceptGameEnding(gameSeriesState, playerId)
      _               <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
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
    } yield sseStream[GameSeriesStateView](stream)
    resultOrError(result)
  }

  def testStream() = Action { reques =>
    import java.time.ZonedDateTime
    import java.time.format.DateTimeFormatter
    import scala.concurrent.duration._
    import scala.language.postfixOps

    val df: DateTimeFormatter = DateTimeFormatter.ofPattern("HH mm ss")
    val tickSource            = Source.tick(0 millis, 500 millis, "TICK")
    val source                = tickSource.map(_ => "data:" + df.format(ZonedDateTime.now()) + "\n\n")
    Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM).withHeaders("Cache-Control" -> "no-transform")
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
      _               <- Either.cond(GameSeriesLogic.isDrawThrowTimingAccepted(gameSeriesState), (), "Draw-throw timing not accepted")
      payload         <- requestJson[DrawThrowCardClientResponse](request)
      card = payload.card
      gameState    <- getGameState(gameSeriesState)
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

  def getGameState(gameSeriesState: GameSeriesState): Either[String, GameState] = {
    (gameSeriesState.state, gameSeriesState.currentGame) match {
      case (GameIsRunning, Some(gs)) => Right(gs)
      case (noCurrentGame, _)        => Left(s"There is no current game: ${noCurrentGame.toString}")
    }
  }
}
