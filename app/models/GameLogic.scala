package models

object GameLogic {

  def playerOrder(gs: GameState): Seq[PlayerId] = gs.players.map(_.id).sorted

  def nextPlayer(gs: GameState): PlayerId = {
    val order        = playerOrder(gs)
    val currentIndex = order.indexOf(gs.currentPlayer)
    if (currentIndex + 1 == gs.players.size)
      order.head
    else
      order(currentIndex + 1)
  }

  // for draw-throw when it's already someone else's turn
  def previousPlayer(gs: GameState): PlayerId = {
    val order        = playerOrder(gs)
    val currentIndex = order.indexOf(gs.currentPlayer)
    if (currentIndex == 0)
      order.last
    else
      order(currentIndex - 1)
  }

  private def validateGameState(
      gs: GameState,
      playerId: PlayerId,
      expectedAction: GameAction
  ): Either[String, GameState] = {
    if (gs.currentPlayer != playerId)
      Left(s"Current player is ${gs.currentPlayer}")
    else if (gs.nextAction != expectedAction)
      Left(s"Next action is ${gs.nextAction}")
    else if (gs.ending.nonEmpty)
      Left(s"This game is already over")
    else
      Right(gs)
  }

  def throwCards(gs: GameState, playerId: PlayerId, cards: Seq[Card]): Either[String, GameState] = {
    for {
      _ <- validateGameState(gs, playerId, Throw)
      _ <- if (isValidCombination(cards)) Right(()) else Left("Not a valid combination")
      player = gs.players.find(_.id == playerId).get
      _ <- if (cards.forall(player.cards.contains)) Right(()) else Left("Player does not have these cards")
    } yield {
      val newPlayer = player.copy(cards = player.cards.filterNot(cards.toSet))
      val newPile   = gs.pile.throwCards(cards)
      gs.copy(
        players = gs.players.map { p => if (p.id == playerId) newPlayer else p },
        nextAction = Draw,
        pile = newPile
      )
    }
  }

  def drawCard(gs: GameState, playerId: PlayerId, source: DrawSource): Either[String, GameState] = {
    for {
      _ <- validateGameState(gs, playerId, Draw)
    } yield {
      val (newCard, drawThrowable, newDeck, newPile) = source match {
        case DeckSource if gs.deck.size > 1 =>
          (gs.deck.head, Some(gs.deck.head), gs.deck.drop(1), gs.pile)
        case DeckSource => // draw the last card, re-shuffle pile into deck
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
        nextAction = Throw,
        pile = newPile,
        deck = newDeck
      )
    }
  }

  def drawThrowCard(gs: GameState, playerId: PlayerId, card: Card): Either[String, GameState] = {
    for {
      _ <- validateGameState(gs, previousPlayer(gs), Draw)
      drawThrowLocation <- if (isValidCombination(card +: gs.pile.top)) Right(Before)
      else if (isValidCombination(gs.pile.top :+ card)) Right((After))
      else Left("Not a valid combination (either left or right of the top)")
      playerCards = gs.players.find(_.id == playerId).get
      _ <- if (playerCards.cards.contains(card)) Right(()) else Left("Player does not have this card")
      drawThrowable <- if (playerCards.drawThrowable.isDefined) Right(playerCards.drawThrowable.get)
      else Left("Player does not have a draw-throwable card")
      _ <- if (drawThrowable == card) Right(()) else Left("This is not the player's drawThrowable card")
    } yield {
      val newPlayer = playerCards.copy(cards = playerCards.cards.filterNot(Set(card)))
      val newPile   = gs.pile.drawThrowCard(card, drawThrowLocation)
      gs.copy(
        players = gs.players.map { p => if (p.id == playerId) newPlayer else p },
        pile = newPile
      )
    }
  }

  def callYaniv(gs: GameState, playerId: PlayerId): Either[String, GameState] =
    for {
      _ <- validateGameState(gs, playerId, Throw)
      player       = gs.players.find(_.id == playerId).get
      playerPoints = player.cards.map(_.endValue).sum
      _ <- if (playerPoints <= YanivPoints) Right(())
      else Left(s"Calling Yaniv is only allowed with <= $YanivPoints points")
    } yield {
      val gameScores       = gs.players.map(p => p.id -> p.cards.map(c => c.endValue).sum).toMap
      val minPointsPlayers = gameScores.groupBy(_._2).toSeq.minBy(_._1)._2.keys.toSeq

      val (ending, playerScore) =
        if (minPointsPlayers.size == 1 && minPointsPlayers.head == playerId)
          (Yaniv(playerId), playerPoints)
        else
          (Asaf(playerId, minPointsPlayers.filter(_ != playerId).head), playerPoints + AsafPenalty)

      val finalScores = gameScores ++ Map(playerId -> playerScore)

      gs.copy(ending = Some(GameResult(ending, finalScores)))
    }

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
}
