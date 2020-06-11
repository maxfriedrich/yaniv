package service

import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.duration._
import de.maxfriedrich.yaniv.ai.{BaselineAI, BaselineAIState}
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

  var ai: BaselineAI = _

  inGame.delay(1.second).runForeach { update =>
    update.state match {
      case WaitingForNextGame(acceptedPlayers) if !acceptedPlayers(playerId) =>
        gameSeriesAction(AcceptNext(playerId))
      case GameIsRunning =>
        for {
          game <- update.currentGame
          newAi = if (game.lastAction.isEmpty)
            makeAI(playerId, game.playerOrder, update.version)
          else
            ai.update(update.version, game)
        } yield {
          ai = newAi
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
  def makeAI(me: PlayerId, opponents: Seq[PlayerId], version: Int): BaselineAI =
    BaselineAI(BaselineAIState.empty(me, opponents, version))
}
