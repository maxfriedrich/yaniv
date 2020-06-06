package de.maxfriedrich.yaniv.game

object GameLogic {

  def playerOrder(gs: GameState): Seq[PlayerId] = gs.players.map(_.id)

  def nextPlayer(gs: GameState): PlayerId = {
    val order        = playerOrder(gs)
    val currentIndex = order.indexOf(gs.currentPlayer)
    if (currentIndex + 1 == gs.players.size)
      order.head
    else
      order(currentIndex + 1)
  }

  private def validateGameState(
      gs: GameState,
      playerId: PlayerId,
      expectedAction: GameActionType,
      drawThrow: Boolean = false
  ): Either[String, GameState] = {
    if (!drawThrow && gs.currentPlayer != playerId)
      Left(s"Current player is ${gs.currentPlayer}")
    else if (drawThrow && gs.drawThrowPlayer.fold(false)(_ != playerId))
      Left(s"$playerId can't draw-throw")
    else if (gs.nextAction != expectedAction)
      Left(s"Next action is ${gs.nextAction}")
    else if (gs.ending.nonEmpty)
      Left(s"This game is already over")
    else
      Right(gs)
  }

  def throwCards(gs: GameState, playerId: PlayerId, cards: Seq[Card]): Either[String, GameState] = {
    for {
      _ <- validateGameState(gs, playerId, ThrowType)
      _ <- Either.cond(cards.distinct.size == cards.size, (), "Duplicate cards")
      _ <- Either.cond(isValidCombination(cards), (), "Not a valid combination")
      player = gs.players.find(_.id == playerId).get
      _ <- Either.cond(cards.forall(player.cards.contains), (), "Player does not have these cards")
    } yield {
      val newPlayers = gs.players.map { p =>
        p.id match {
          case id if id == playerId => p.copy(cards = player.cards.filterNot(cards.toSet), drawThrowable = None)
          case _                    => p.copy(drawThrowable = None)
        }
      }
      gs.copy(
        players = newPlayers,
        drawThrowPlayer = None,
        nextAction = DrawType,
        lastAction = Some(Throw(cards)),
        pile = gs.pile.throwCards(cards)
      )
    }
  }

  def drawCard(gs: GameState, playerId: PlayerId, source: DrawSource): Either[String, GameState] = {
    for {
      _ <- validateGameState(gs, playerId, DrawType)
    } yield {
      val (newCard, drawThrowable, newDeck, newPile) = source match {
        case DeckSource if gs.deck.size > 1 =>
          (gs.deck.head, Some(gs.deck.head), gs.deck.drop(1), gs.pile)
        case DeckSource => // draw the last card, re-shuffle pile into deck
          // TODO: this should be reflected in the current game state!
          val reshuffled = Shuffle.reshuffle(gs.pile)
          (gs.deck.head, Some(gs.deck.head), reshuffled.deck, reshuffled.pile)
        case PileSource(card) =>
          if (!gs.pile.drawable.filter(_.drawable).map(_.card).contains(card))
            return Left(s"Card ${card.id} is not drawable")
          else
            (card, None, gs.deck, gs.pile.drawCard(card))
      }

      val playerCards = gs.players.find(_.id == playerId).get
      val newPlayer   = playerCards.copy(cards = playerCards.cards :+ newCard, drawThrowable = drawThrowable)
      gs.copy(
        players = gs.players.map { p => if (p.id == playerId) newPlayer else p },
        currentPlayer = nextPlayer(gs),
        drawThrowPlayer = Some(playerId),
        nextAction = ThrowType,
        lastAction = Some(Draw(source)),
        pile = newPile,
        deck = newDeck
      )
    }
  }

  def drawThrowCard(gs: GameState, playerId: PlayerId, card: Card): Either[String, GameState] = {
    for {
      _                 <- validateGameState(gs, playerId, ThrowType, drawThrow = true)
      drawThrowLocation <- findDrawThrowLocation(card, gs.pile.top)
      playerCards = gs.players.find(_.id == playerId).get
      _ <- Either.cond(playerCards.cards.contains(card), (), "Player does not have this card")
      drawThrowable <- Either.cond(
        playerCards.drawThrowable.isDefined,
        playerCards.drawThrowable.get,
        "Player does not have a draw-throwable card"
      )
      _ <- Either.cond(drawThrowable == card, (), "This is not the player's drawThrowable card")
    } yield {
      val newCards   = playerCards.cards.filterNot(Set(card))
      val newPlayer  = playerCards.copy(cards = newCards, drawThrowable = None)
      val newPlayers = gs.players.map { p => if (p.id == playerId) newPlayer else p }
      val newPile    = gs.pile.drawThrowCard(card, drawThrowLocation)
      val ending =
        if (newPlayer.cards.isEmpty) Some(GameResult(EmptyHand(playerId), computeGameScores(newPlayers))) else None
      gs.copy(
        players = gs.players.map { p => if (p.id == playerId) newPlayer else p },
        drawThrowPlayer = None,
        lastAction = Some(DrawThrow(card)),
        pile = newPile,
        ending = ending
      )
    }
  }

  def callYaniv(gs: GameState, playerId: PlayerId): Either[String, GameState] =
    for {
      _ <- validateGameState(gs, playerId, ThrowType)
      player       = gs.players.find(_.id == playerId).get
      playerPoints = player.cards.map(_.endValue).sum
      _ <- Either.cond(
        playerPoints <= gs.config.yanivMaxPoints,
        (),
        s"Calling Yaniv is only allowed with <= ${gs.config.yanivMaxPoints} points"
      )
    } yield {
      val gameScores       = computeGameScores(gs.players)
      val minPointsPlayers = gameScores.groupBy(_._2).toSeq.minBy(_._1)._2.keys.toSeq

      val (ending, playerScore) =
        if (minPointsPlayers.size == 1 && minPointsPlayers.head == playerId)
          (Yaniv(playerId, playerPoints), playerPoints)
        else
          (
            Asaf(playerId, playerPoints, minPointsPlayers.filter(_ != playerId).head, gameScores.values.min),
            playerPoints + gs.config.asafPenalty
          )

      val finalScores = gameScores ++ Map(playerId -> playerScore)

      gs.copy(ending = Some(GameResult(ending, finalScores)))
    }

  def computeGameScores(players: Seq[PlayerCards]): Map[PlayerId, Int] =
    players.map(p => p.id -> p.cards.map(c => c.endValue).sum).toMap

  def isValidCombination(cards: Seq[Card]): Boolean = {
    if (cards.size == 1) {
      true
    } else {
      val jokers = cards.collect { case j: Joker       => j }
      val others = cards.collect { case o: RegularCard => o }

      if (jokers.size == 3 && others.isEmpty)
        true
      else if (others.map(_.value.position).distinct.size == 1)
        true
      else if (cards.size >= 3 && others.map(_.suit).distinct.size == 1)
        checkStreet(cards)
      else
        false
    }
  }

  private def checkStreet(cards: Seq[Card]): Boolean = {
    val withoutLeftJokers = cards.dropWhile { c => c.isInstanceOf[Joker] }
    withoutLeftJokers
      .drop(1)
      .foldLeft(
        (
          true,
          withoutLeftJokers.head
            .asInstanceOf[RegularCard]
            .value
            .position
        )
      ) { (acc, card) =>
        card match {
          case Joker(_) => (acc._1, acc._2 + 1)
          case c: RegularCard if acc._2 + 1 == c.value.position =>
            (acc._1, acc._2 + 1)
          case _ => (false, acc._2 + 1)
        }
      }
      ._1
  }

  // preferring after because it seems more natural
  private def findDrawThrowLocation(card: Card, top: Seq[Card]): Either[String, DrawThrowLocation] = {
    if (isValidCombination(top :+ card)) Right(After)
    else if (isValidCombination(card +: top)) Right(Before)
    else Left("Not a valid combination (either left or right of the top)")
  }
}
