package models.series

import java.time.{Duration, LocalDateTime}

import models.{Asaf, Card, EmptyHand, GameState, PlayerCards, PlayerId, Yaniv}

object GameSeriesLogic {
  def addPlayer(gss: GameSeriesState, playerInfo: PlayerInfo): Either[String, GameSeriesState] =
    for {
      _ <- Either.cond(gss.state == WaitingForSeriesStart, (), "Game series was already started")
    } yield gss.copy(players = gss.players :+ playerInfo, scores = gss.scores ++ Map(playerInfo.id -> 0))

  def removePlayer(gss: GameSeriesState, playerId: PlayerId): Either[String, GameSeriesState] =
    for {
      _ <- Either.cond(gss.state == WaitingForSeriesStart, (), "Game series was already started")
    } yield {
      val newPlayers = gss.players.filter(_.id != playerId)
      val newScores  = gss.scores.filter(_._1 != playerId)
      gss.copy(players = newPlayers, scores = newScores)
    }

  def startSeries(gss: GameSeriesState): Either[String, GameSeriesState] =
    for {
      _ <- Either.cond(gss.state == WaitingForSeriesStart, (), "Game series was already started")
    } yield gss.copy(state = GameIsRunning, currentGame = Some(GameState.newGame(gss.config.gameConfig, gss.players)))

  def isDrawThrowTimingAccepted(gss: GameSeriesState): Boolean = {
    val timeDiff = Duration.between(gss.timestamp, LocalDateTime.now()).toMillis
    println(s"timeDiff: $timeDiff")
    timeDiff < gss.config.drawThrowMillis
  }

  def acceptGameEnding(gss: GameSeriesState, player: PlayerId): Either[String, GameSeriesState] =
    for {
      newAccepted <- gss.state match {
        case WaitingForNextGame(accepted) if accepted.contains(player) =>
          Left("Player has already accepted next game")
        case WaitingForNextGame(accepted) => Right(accepted ++ Set(player))
        case _                            => Left("There is no next game to accept")
      }
      allAccepted = newAccepted.size == gss.players.size
    } yield {
      val (newState, newGame) =
        if (allAccepted) {
          (GameIsRunning, Some(GameState.newGame(gss.config.gameConfig, gss.players, startingPlayer = gss.lastWinner)))
        } else {
          (WaitingForNextGame(newAccepted), gss.currentGame)
        }
      gss.copy(state = newState, currentGame = newGame)
    }

  def updateGameState(gss: GameSeriesState, gs: GameState): GameSeriesState = gs.ending match {
    case Some(result) =>
      val newScoresBeforeRules = gss.scores.map {
        case (playerId, oldScore) => playerId -> (oldScore + result.points(playerId))
      }
      val winner = result.ending match {
        case Yaniv(caller, _)      => caller
        case Asaf(_, _, winner, _) => winner
        case EmptyHand(player)     => player
      }
      val excludedFromRules = result.points.filter { case (playerId, score) => score == 0 }.keys.toSet
      val newScoresAfterRules =
        applyPointRules(gss.config.pointRules, gs.players, newScoresBeforeRules, excludedFromRules)

      val scoresDiff = gss.scores.map {
        case (playerId, oldScore) =>
          val firstDiff  = newScoresBeforeRules(playerId) - oldScore
          val secondDiff = newScoresAfterRules(playerId) - newScoresBeforeRules(playerId)
          playerId -> Seq(firstDiff, secondDiff).filter(_ != 0)
      }

      val newState =
        checkSeriesEnding(newScoresAfterRules, gss.config.losingPoints).getOrElse(WaitingForNextGame(Set.empty))
      gss.copy(
        state = newState,
        currentGame = Some(gs),
        lastWinner = Some(winner),
        scores = newScoresAfterRules,
        scoresDiff = scoresDiff
      )
    case None => gss.copy(currentGame = Some(gs))
  }

  private[series] def applyPointRules(
      rules: Seq[PointRule],
      players: Seq[PlayerCards],
      scores: Map[PlayerId, Int],
      excludedFromScoreRules: Set[PlayerId]
  ): Map[PlayerId, Int] = {
    val playerCards = players.map(p => p.id -> p.cards)

    rules.foldLeft(scores) { (acc, rule) =>
      playerCards.map {
        case (playerId, hand) =>
          playerId -> applyRule(rule, hand.toSet, acc(playerId), excludedFromScoreRules(playerId))
      }.toMap
    }
  }

  private[series] def applyRule(rule: PointRule, hand: Set[Card], score: Int, excludedFromScoreRules: Boolean): Int =
    rule match {
      case PointRule(Hand(h), newScore) if h == hand                              => newScore
      case PointRule(Score(s), newScore) if s == score && !excludedFromScoreRules => newScore
      case _                                                                      => score
    }

  private[series] def checkSeriesEnding(scores: Map[PlayerId, Int], losingScore: Int): Option[GameOver] = {
    if (scores.values.exists(_ > losingScore))
      Some(GameOver(scores.minBy(_._2)._1)) // currently only one winner
    else
      None
  }
}
