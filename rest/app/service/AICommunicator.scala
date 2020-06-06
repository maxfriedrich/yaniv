package service

import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.duration._
import de.maxfriedrich.yaniv.ai.BaselineAI
import de.maxfriedrich.yaniv.game.{GameAction, PlayerId}
import de.maxfriedrich.yaniv.game.series.{AcceptNext, GameSeriesAction, GameSeriesStateView, WaitingForNextGame}

class AICommunicator(
    playerId: PlayerId,
    inGame: Source[GameSeriesStateView, _],
    gameAction: GameAction => Either[String, GameSeriesStateView],
    gameSeriesAction: GameSeriesAction => Either[String, Unit]
)(implicit mat: Materializer) {
  val ai = new BaselineAI()

  inGame.delay(1.second).runForeach { update =>
    update.state match {
      case WaitingForNextGame(acceptedPlayers) if !acceptedPlayers(playerId) =>
        gameSeriesAction(AcceptNext(playerId))
      case _ =>
        update.currentGame match {
          case Some(game) if game.currentPlayer == playerId =>
            val action = ai.playTurn(game)
            gameAction(action)
          case Some(game) if game.drawThrowPlayer.fold(false)(_ == playerId) =>
            val action = ai.playDrawThrow(game)
            action.map(gameAction)
          case _ => ()
        }
    }
  }
}
