package service

import akka.stream.scaladsl.Source
import de.maxfriedrich.yaniv.game.series.{
  GameSeriesPreStartInfo,
  GameSeriesState,
  GameSeriesStateView,
  PlayerInfo,
  WaitingForSeriesStart
}
import de.maxfriedrich.yaniv.game.{DummyGame, GameSeriesId, PlayerId}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class GamesStorageService(
    notifyPreGame: Notify[GameSeriesId, GameSeriesPreStartInfo],
    notifyInGame: Notify[(GameSeriesId, PlayerId), GameSeriesStateView]
) {

  private val gameSeriesStates = mutable.Map.empty[GameSeriesId, mutable.Buffer[GameSeriesState]]
  gameSeriesStates += DummyGame.dummyGame
  gameSeriesStates += DummyGame.drawThrowTest
  gameSeriesStates += DummyGame.betweenGames
  gameSeriesStates += DummyGame.fiveCardsOnPile
  gameSeriesStates += DummyGame.gameOver
  gameSeriesStates += DummyGame.singleCards

  def getGameSeriesPreStartInfo(gameSeriesId: GameSeriesId): Either[String, GameSeriesPreStartInfo] =
    for {
      series <- validateGameSeries(gameSeriesId)
    } yield GameSeriesPreStartInfo.fromGameSeriesState(series.last)

  def getGameSeriesState(gameSeriesId: GameSeriesId): Either[String, GameSeriesState] =
    for {
      series <- validateGameSeries(gameSeriesId)
    } yield series.last

  def getGameSeriesStateView(gameSeriesId: GameSeriesId, playerId: PlayerId): Either[String, GameSeriesStateView] =
    for {
      series <- validateGameSeries(gameSeriesId)
      _      <- validateGamePlayer(series, playerId)
    } yield GameSeriesStateView.fromGameSeriesState(series.last, playerId)

  def getGameSeriesStateStream(gameSeriesId: GameSeriesId, playerId: PlayerId)(
      implicit ec: ExecutionContext
  ): Either[String, Source[GameSeriesStateView, _]] =
    for {
      series <- validateGameSeries(gameSeriesId)
      _      <- validateGamePlayer(series, playerId)
    } yield notifyInGame.signUp((gameSeriesId, playerId))

  def getGameSeriesPreStartInfoStream(
      gameSeriesId: GameSeriesId
  )(implicit ec: ExecutionContext): Either[String, Source[GameSeriesPreStartInfo, _]] =
    for {
      series <- validateGameSeries(gameSeriesId)
      _ = Either.cond(series.last.state == WaitingForSeriesStart, (), s"Game $gameSeriesId has already started")
    } yield notifyPreGame.signUp(gameSeriesId)

  def create(initialState: GameSeriesState): Either[String, Unit] = {
    val gameSeriesId = initialState.id
    for {
      _ <- Either.cond(!gameSeriesStates.contains(gameSeriesId), (), s"Game $gameSeriesId already exists")
    } yield {
      gameSeriesStates += gameSeriesId -> mutable.Buffer(initialState)
    }
  }

  def update(gameSeriesId: GameSeriesId, gameSeriesState: GameSeriesState): Either[String, Unit] = {
    synchronized {
      for {
        series <- validateGameSeries(gameSeriesId)
        _ <- Either.cond(
          gameSeriesState.version > series.last.version,
          (),
          s"Game state version ${gameSeriesState.version} is too old, the current version is ${series.last.version}"
        )
      } yield {
        gameSeriesStates += gameSeriesId -> (series :+ gameSeriesState)
        notifyPreGame.sendUpdate(gameSeriesId, GameSeriesPreStartInfo.fromGameSeriesState(gameSeriesState))
        gameSeriesState.players.foreach { player =>
          notifyInGame.sendUpdate(
            (gameSeriesId, player.id),
            GameSeriesStateView.fromGameSeriesState(gameSeriesState, player.id)
          )
        }
      }
    }
  }

  private def validateGameSeries(gameSeriesId: GameSeriesId): Either[String, mutable.Buffer[GameSeriesState]] =
    gameSeriesStates.get(gameSeriesId) match {
      case Some(states) if states.nonEmpty => Right(states)
      case _                               => Left(s"Game series $gameSeriesId does not exist")
    }

  private def validateGamePlayer(
      gameSeries: mutable.Buffer[GameSeriesState],
      playerId: PlayerId
  ): Either[String, PlayerInfo] =
    gameSeries.last.players.find(_.id == playerId) match {
      case Some(playerInfo) => Right(playerInfo)
      case _                => Left(s"Player $playerId is not a part of game ${gameSeries.last.id}")
    }
}
