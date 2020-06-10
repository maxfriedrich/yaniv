package service

import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.duration._
import de.maxfriedrich.yaniv.ai.{AI, BaselineAI}
import de.maxfriedrich.yaniv.game.{GameAction, PlayerId}
import de.maxfriedrich.yaniv.game.series.{
  AcceptNext,
  GameIsRunning,
  GameSeriesAction,
  GameSeriesStateView,
  WaitingForNextGame
}

class AICommunicator(
    playerId: PlayerId,
    inGame: Source[GameSeriesStateView, _],
    gameAction: GameAction => Either[String, GameSeriesStateView],
    gameSeriesAction: GameSeriesAction => Either[String, Unit]
)(implicit mat: Materializer) {

  import AICommunicator._

  var ai: AI = _

  inGame.delay(1.second).runForeach { update =>
    update.state match {
      case WaitingForNextGame(acceptedPlayers) if !acceptedPlayers(playerId) =>
        gameSeriesAction(AcceptNext(playerId))
      case GameIsRunning =>
        for {
          game <- update.currentGame
          _ = if (game.lastAction.isEmpty) ai = makeAI(playerId, game.playerOrder)
          _ = ai.update(update.version, game)
        } yield {
          if (game.currentPlayer == playerId) {
            val action = ai.playTurn(game)
            gameAction(action)
          } else if (game.drawThrowPlayer.fold(false)(_ == playerId)) {
            val action = ai.playDrawThrow(game)
            action.map(gameAction)
          }
        }
    }
  }
}

object AICommunicator {
  def makeAI(me: PlayerId, opponents: Seq[PlayerId]): AI = new BaselineAI(me, opponents)
}
