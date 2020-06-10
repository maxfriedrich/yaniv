package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game._

class BaselineAI(me: PlayerId, playerOrder: Seq[PlayerId]) extends AI {

  import BaselineAI._

  val knownCards       = new KnownCardsService(playerOrder.filterNot(Set(me)))
  var lastVersion: Int = -1
  val nextPlayer: PlayerId = {
    val myIndex = playerOrder.indexOf(me)
    if (myIndex + 1 == playerOrder.size)
      playerOrder.head
    else
      playerOrder(myIndex + 1)
  }

  def reset(): Unit = ()

  def update(version: Int, gameStateView: GameStateView): Unit = {
    if (version == lastVersion + 1) {
      knownCards.update(gameStateView.otherPlayers, gameStateView.lastAction)
    } else if (version > lastVersion + 1) {
      // An update was missing, better just reset the ledger
      knownCards.reset()
    }
  }

  def playTurn(gameStateView: GameStateView): GameAction = gameStateView.nextAction match {
    case ThrowType => playThrow(gameStateView)
    case DrawType  => playDraw(gameStateView)
  }

  def playThrow(gameStateView: GameStateView): GameAction = {
    val myCards = gameStateView.me.cards
    if (myCards.map(_.endValue).sum <= callYanivThreshold)
      Yaniv
    else {
      val cardsWithoutPlanned = {
        val step1 = myCards.filterNot(cardsToHoldBackForNextThrow(myCards, gameStateView.pile.top))
        step1.filterNot(cardsToHoldBackForNextPlayer(step1, knownCards.get(nextPlayer)))
      }
      // fall back to all cards if there are no more unplanned cards
      val cardsToUse = if (cardsWithoutPlanned.nonEmpty) cardsWithoutPlanned else myCards
      Throw(AILogic.bestCombination(cardsToUse))
    }
  }

  def playDrawThrow(gameStateView: GameStateView): Option[GameAction] =
    for {
      card <- gameStateView.me.drawThrowable
      if AILogic.doesMatchOutside(gameStateView.pile.top, card)
    } yield DrawThrow(card)

  def callYanivThreshold: Int = 3
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
}
