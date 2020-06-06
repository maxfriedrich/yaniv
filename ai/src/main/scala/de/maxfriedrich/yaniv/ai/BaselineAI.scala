package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game._

class BaselineAI extends AI {
  import BaselineAI._

  def update(gameStateView: GameStateView): Unit = ()

  def playTurn(gameStateView: GameStateView): GameAction = gameStateView.nextAction match {
    case ThrowType => playThrow(gameStateView)
    case DrawType  => playDraw(gameStateView)
  }

  def playDrawThrow(gameStateView: GameStateView): Option[GameAction] =
    gameStateView.me.drawThrowable.fold(Option.empty[GameAction]) { drawThrowable =>
      if (AILogic.doesMatchOutside(gameStateView.pile.top, drawThrowable))
        Some(DrawThrow(drawThrowable))
      else
        None
    }
}

object BaselineAI {
  def playThrow(gameStateView: GameStateView): GameAction = {
    val myCards = gameStateView.me.cards
    if (myCards.map(_.endValue).sum <= 3)
      Yaniv
    else {
      val cardsWithoutPlanned = myCards.filterNot(cardsToHoldBack(myCards, gameStateView.pile.top))
      // fall back to all cards if there are no more unplanned cards
      val cardsToUse = if (cardsWithoutPlanned.nonEmpty) cardsWithoutPlanned else myCards
      Throw(AILogic.bestCombination(cardsToUse))
    }
  }

  def playDraw(gameStateView: GameStateView): GameAction = {
    val drawableCards = gameStateView.pile.drawable.map(_.card)
    val pileCard      = drawJokerFromPile(drawableCards).orElse(cardToDrawFromPile(gameStateView.me.cards, drawableCards))
    pileCard match {
      case Some(card) => Draw(PileSource(card))
      case _          => Draw(DeckSource)
    }
  }

  def drawJokerFromPile(drawableCards: Seq[Card]): Option[Card] =
    drawableCards.collectFirst { case j: Joker => j }

  def cardsToHoldBack(cards: Seq[Card], pileTop: Seq[Card]): Set[Card] = {
    AILogic
      .combinationsWithAdditionalCards(cards, Seq(pileTop.head, pileTop.last).distinct)
      .collectFirst {
        case (_, combination) => combination.filter(cards.contains).toSet
      }
      .getOrElse(Set.empty)
  }

  def cardToDrawFromPile(cards: Seq[Card], drawableCards: Seq[Card]): Option[Card] = {
    AILogic.combinationsWithAdditionalCards(cards, drawableCards).collectFirst {
      case (drawable, _) => drawable
    }
  }
}
