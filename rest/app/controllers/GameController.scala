package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import de.maxfriedrich.yaniv.game.{Draw, DrawThrow, GameAction, GameLogic, GameState, Throw}
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

  val gamesService = new GamesService()
  val idService    = new IdService()

  def newGame() = Action {
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

  def remove(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.removePlayer(gameSeriesState, playerId)
      _               <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def preStartInfo(gameSeriesId: String) = Action {
    val result = for {
      stateView <- gamesService.getGameSeriesPreStartInfo(gameSeriesId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def preStartInfoStream(gameSeriesId: String) = Action {
    val result = for {
      stream <- gamesService.getGameSeriesPreStartInfoStream(gameSeriesId)
    } yield sseStream[GameSeriesPreStartInfo](stream)
    resultOrError(result)
  }

  def start(gameSeriesId: String) = Action {
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.startSeries(gameSeriesState)
      _               <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def next(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      newSeriesState  <- GameSeriesLogic.acceptGameEnding(gameSeriesState, playerId)
      _               <- gamesService.update(gameSeriesId, newSeriesState)
    } yield Ok(Json.toJson(Map("ok" -> "ok")))
    resultOrError(result)
  }

  def state(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      stateView <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield Ok(Json.toJson(stateView))
    resultOrError(result)
  }

  def stateStream(gameSeriesId: String, playerId: String) = Action {
    val result = for {
      stream <- gamesService.getGameSeriesStateStream(gameSeriesId, playerId)
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
      result <- action match {
        case "throw"     => performGameActionWithPayload(gameSeriesId, playerId, request, performThrow)
        case "draw"      => performGameActionWithPayload(gameSeriesId, playerId, request, performDraw)
        case "drawThrow" => performGameActionWithPayload(gameSeriesId, playerId, request, performDrawThrow)
        case "yaniv"     => performYaniv(gameSeriesId, playerId)
      }
    } yield Ok(Json.toJson(result))
    resultOrError(result)
  }

  private def performThrow(
      gameSeriesId: String,
      playerId: String,
      payload: Throw
  ): Either[String, GameSeriesStateView] =
    for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      cards = payload.cards
      gameState      <- getGameState(gameSeriesState)
      newGameState   <- GameLogic.throwCards(gameState, playerId, cards)
      newSeriesState <- GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _              <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView  <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield gameStateView

  private def performDraw(gameSeriesId: String, playerId: String, payload: Draw): Either[String, GameSeriesStateView] =
    for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      source = payload.source
      gameState      <- getGameState(gameSeriesState)
      newGameState   <- GameLogic.drawCard(gameState, playerId, source)
      newSeriesState <- GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _              <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView  <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield gameStateView

  private def performDrawThrow(
      gameSeriesId: String,
      playerId: String,
      payload: DrawThrow
  ): Either[String, GameSeriesStateView] =
    for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      _               <- Either.cond(GameSeriesLogic.isDrawThrowTimingAccepted(gameSeriesState), (), "Draw-throw timing not accepted")
      card = payload.card
      gameState      <- getGameState(gameSeriesState)
      newGameState   <- GameLogic.drawThrowCard(gameState, playerId, card)
      newSeriesState <- GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _              <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView  <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield gameStateView

  private def performYaniv(gameSeriesId: String, playerId: String): Either[String, GameSeriesStateView] =
    for {
      gameSeriesState <- gamesService.getGameSeriesState(gameSeriesId)
      gameState       <- getGameState(gameSeriesState)
      newGameState    <- GameLogic.callYaniv(gameState, playerId)
      newSeriesState  <- GameSeriesLogic.updateGameState(gameSeriesState, newGameState)
      _               <- gamesService.update(gameSeriesId, newSeriesState)
      gameStateView   <- gamesService.getGameSeriesStateView(gameSeriesId, playerId)
    } yield gameStateView
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

  private def performGameActionWithPayload[A <: GameAction: Reads](
      gameSeriesId: String,
      playerId: String,
      request: Request[AnyContent],
      action: (String, String, A) => Either[String, GameSeriesStateView]
  ) =
    for {
      payload <- requestJson[A](request)
      result  <- action(gameSeriesId, playerId, payload)
    } yield result
}
