package models.series

import models.{Card, GameState, PlayerCards, PlayerId}

object GameSeriesLogic {
  def addPlayer(gss: GameSeriesState, playerInfo: PlayerInfo): Either[String, GameSeriesState] =
    for {
      _ <- gss.currentGame match {
        case Left(WaitingForSeriesStart) => Right(())
        case _                           => Left("Game series was already started")
      }
    } yield gss.copy(players = gss.players :+ playerInfo, scores = gss.scores ++ Map(playerInfo.id -> 0))

  def startSeries(gss: GameSeriesState): Either[String, GameSeriesState] =
    for {
      _ <- gss.currentGame match {
        case Left(WaitingForSeriesStart) => Right(())
        case _                           => Left("The series is already running")
      }
    } yield gss.copy(state = Right(GameState.newGame(gss.config.gameConfig, gss.players)))

  def acceptGameEnding(gss: GameSeriesState, player: PlayerId): Either[String, GameSeriesState] =
    for {
      newAccepted <- gss.currentGame match {
        case Left(WaitingForNextGame(accepted)) if accepted.contains(player) =>
          Left("Player has already accepted next game")
        case Left(WaitingForNextGame(accepted)) => Right(accepted ++ Set(player))
        case _                                  => Left("There is no next game to accept")
      }
      allAccepted = newAccepted.size == gss.players.size
    } yield {
      val newState =
        if (allAccepted) Right(GameState.newGame(gss.config.gameConfig, gss.players))
        else Left(WaitingForNextGame(newAccepted))
      gss.copy(state = newState)
    }

  def updateGameState(gss: GameSeriesState, gs: GameState): GameSeriesState = gs.ending match {
    case Some(ending) =>
      val newScoresBeforeRules = gss.scores.map {
        case (playerId, oldScore) => playerId -> (oldScore + ending.points.getOrElse(playerId, 0))
      }
      val newScoresAfterRules = applyPointRules(gss.config.pointRules, gs.players, newScoresBeforeRules)
      val newState = checkEndingScores(newScoresAfterRules, gss.config.losingPoints) match {
        case Some(go) => Left(go)
        case _        => Right(gs)
      }
      gss.copy(state = newState)
    case None => gss.copy(state = Right(gs))
  }

  private[series] def applyPointRules(
      rules: Seq[PointRule],
      players: Seq[PlayerCards],
      scores: Map[PlayerId, Int]
  ): Map[PlayerId, Int] = {
    val playerCards = players.map(p => p.id -> p.cards)

    rules.foldLeft(scores) { (acc, rule) =>
      playerCards.map {
        case (playerId, hand) => playerId -> applyRule(rule, hand.toSet, acc(playerId))
      }.toMap
    }
  }

  private[series] def applyRule(rule: PointRule, hand: Set[Card], score: Int): Int = rule match {
    case PointRule(Hand(h), newScore) if h == hand   => newScore
    case PointRule(Score(s), newScore) if s == score => newScore
    case _                                           => score
  }

  private[series] def checkEndingScores(scores: Map[PlayerId, Int], losingScore: Int): Option[GameOver] = {
    if (scores.values.exists(_ > losingScore))
      Some(GameOver(scores.minBy(_._2)._1)) // currently only one winner
    else
      None
  }
}
