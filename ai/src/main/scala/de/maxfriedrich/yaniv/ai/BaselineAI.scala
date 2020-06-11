package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.ai.BaselineAIState.KnownCardsMap
import de.maxfriedrich.yaniv.game._

case class BaselineAIState(me: PlayerId, afterMe: PlayerId, version: Int, knownCards: KnownCardsMap)

object BaselineAIState {
  type KnownCardsMap = Map[PlayerId, Set[Card]]

  def empty(me: PlayerId, playerOrder: Seq[PlayerId], version: Int): BaselineAIState = {
    val afterMe: PlayerId = {
      val myIndex = playerOrder.indexOf(me)
      if (myIndex + 1 == playerOrder.size)
        playerOrder.head
      else
        playerOrder(myIndex + 1)
    }
    val knownCards = playerOrder.filterNot(Set(me)).map(p => p -> Set.empty[Card]).toMap
    BaselineAIState(me, afterMe, version, knownCards)
  }
}

case class BaselineAI(state: BaselineAIState) extends AI[BaselineAIState] {
  import BaselineAI._

  def update(version: Int, gameStateView: GameStateView): BaselineAI = {
    if (version == state.version + 1) {
      val newKnownCards = gameStateView.lastAction match {
        case Some(actionWithPlayer) if actionWithPlayer.player != state.me =>
          val newPlayerCards =
            updatePlayerKnownCards(state.knownCards(actionWithPlayer.player), actionWithPlayer.gameAction)
          state.knownCards ++ Map(actionWithPlayer.player -> newPlayerCards)
        case _ => state.knownCards
      }
      BaselineAI(state.copy(knownCards = newKnownCards, version = version))
    } else if (version > state.version + 1) {
      // An update in between was missing, better just reset the known cards
      BaselineAI(state.copy(knownCards = state.knownCards.mapValues(_ => Set.empty[Card]), version = version))
    } else {
      this
    }
  }

  def playTurn(gameStateView: GameStateView): GameAction = gameStateView.nextAction match {
    case ThrowType =>
      if (shouldCallYaniv(gameStateView, state.knownCards))
        Yaniv
      else
        playThrow(gameStateView, state.knownCards(state.afterMe))
    case DrawType => playDraw(gameStateView)
  }

  def playDrawThrow(gameStateView: GameStateView): Option[GameAction] =
    for {
      card <- gameStateView.me.drawThrowable
      if AILogic.doesMatchOutside(gameStateView.pile.top, card)
    } yield DrawThrow(card)

}

object BaselineAI {
  def playDraw(gameStateView: GameStateView): GameAction = {
    val drawableCards = gameStateView.pile.drawable.filter(_.drawable).map(_.card)
    val pileCard      = drawJokerFromPile(drawableCards).orElse(cardToDrawFromPile(gameStateView.me.cards, drawableCards))
    pileCard match {
      case Some(card) => Draw(PileSource(card))
      case _          => Draw(DeckSource)
    }
  }

  def drawJokerFromPile(drawableCards: Seq[Card]): Option[Card] =
    drawableCards.collectFirst { case j: Joker => j }

  def cardsToHoldBackForNextThrow(cards: Seq[Card], pileTop: Seq[Card]): Set[Card] = {
    val combinations = AILogic
      .combinationsWithAdditionalCards(cards, Seq(pileTop.head, pileTop.last).distinct)
      .map(_._2)

    if (combinations.nonEmpty)
      combinations.maxBy(_.map(_.endValue).sum).filter(cards.contains).toSet
    else
      Set.empty
  }

  def shouldCallYaniv(gameStateView: GameStateView, knownCards: KnownCardsMap): Boolean = {
    val myScore = gameStateView.me.cards.map(_.endValue).sum
    if (myScore > 5) {
      false
    } else if (minimumKnownScore(knownCards, gameStateView.otherPlayers).fold(false)(myScore > _)) {
      false
    } else if (myScore < minimumKnownScoreLowerBound(knownCards)) {
      true
    } else {
      // TODO: decide based on turn and numCards features
      myScore <= 3
    }
  }

  def minimumKnownScore(knownCards: KnownCardsMap, otherPlayers: Seq[PlayerCardsView]): Option[Int] = {
    val numCards = otherPlayers.map(player => player.id -> player.numCards).toMap
    val fullyKnown = knownCards.collect {
      case (id, known) if (known.size == numCards(id)) => known.map(_.endValue).sum
    }.toSeq
    if (fullyKnown.isEmpty)
      None
    else
      Some(fullyKnown.min)
  }

  def minimumKnownScoreLowerBound(knownCards: KnownCardsMap): Int =
    knownCards.values.map(_.map(_.endValue).sum).min

  def playThrow(gameStateView: GameStateView, playerAfterMeKnownCards: Set[Card]): GameAction = {
    val myCards = gameStateView.me.cards

    val cardsWithoutPlanned = myCards.filterNot(cardsToHoldBackForNextThrow(myCards, gameStateView.pile.top))
    val avoidOutside        = cardsToHoldBackForNextPlayer(cardsWithoutPlanned, playerAfterMeKnownCards)
    // fall back to all cards if there are no more unplanned cards
    val cardsToUse = if (cardsWithoutPlanned.nonEmpty) cardsWithoutPlanned else myCards
    Throw(AILogic.bestCombination(cardsToUse, avoidOutside))
  }

  def cardsToHoldBackForNextPlayer(cards: Seq[Card], nextPlayerKnown: Set[Card]): Set[Card] = {
    if (nextPlayerKnown.size == 1) {
      Set.empty
    } else {
      AILogic
        .combinationsWithAdditionalCards(nextPlayerKnown.toSeq, cards)
        .flatMap(_._2)
        .toSet
    }
  }

  def cardToDrawFromPile(cards: Seq[Card], drawableCards: Seq[Card]): Option[Card] = {
    AILogic.combinationsWithAdditionalCards(cards, drawableCards).collectFirst {
      case (drawable, _) => drawable
    }
  }

  def updatePlayerKnownCards(playerCards: Set[Card], gameAction: GameAction): Set[Card] = gameAction match {
    case Draw(PileSource(card)) => playerCards ++ Set(card)
    case Throw(cards)           => playerCards -- cards
    case DrawThrow(card)        => playerCards -- Set(card)
    case _                      => playerCards
  }
}
