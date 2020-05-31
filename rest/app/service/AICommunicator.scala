package service

import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.duration._
import de.maxfriedrich.yaniv.ai.BaselineAI
import de.maxfriedrich.yaniv.game.{GameAction, GameSeriesId, PlayerId}
import de.maxfriedrich.yaniv.game.series.{AcceptNext, GameSeriesAction, GameSeriesStateView, WaitingForNextGame}

class AICommunicator(
    playerId: PlayerId,
    inGame: Source[GameSeriesStateView, _],
    gameAction: (GameSeriesId, PlayerId, GameAction) => Either[String, GameSeriesStateView],
    gameSeriesAction: (GameSeriesId, GameSeriesAction) => Either[String, Unit]
)(implicit mat: Materializer) {
  val ai = new BaselineAI()

  inGame.delay(1.second).runForeach { update =>
    println(s"AI communicator got in-game update: $update")

    update.state match {
      case WaitingForNextGame(acceptedPlayers) if !acceptedPlayers(playerId) =>
        gameSeriesAction(update.id, AcceptNext(playerId))
      case _ =>
        update.currentGame match {
          case Some(game) if game.currentPlayer == playerId =>
            val action = ai.play(game)
            println(action)
            gameAction(update.id, playerId, action)
          case _ => ()
        }
    }
  }
}
