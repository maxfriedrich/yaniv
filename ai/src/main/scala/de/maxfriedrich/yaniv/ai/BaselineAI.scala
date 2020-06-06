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
      Throw(AILogic.bestCombination(myCards))
    }
  }

  def playDraw(gameStateView: GameStateView): GameAction = {
    val drawableCards = gameStateView.pile.drawable.map(_.card)
    val pileCard = drawJokerFromPile(drawableCards).orElse(cardToDrawFromPile(gameStateView.me.cards, drawableCards))
    pileCard match {
      case Some(card) => Draw(PileSource(card))
      case _          => Draw(DeckSource)
    }
  }

  def drawJokerFromPile(drawableCards: Seq[Card]): Option[Card] =
    drawableCards.collectFirst { case j: Joker => j }

  def cardToDrawFromPile(cards: Seq[Card], drawableCards: Seq[Card]): Option[Card] = {
    AILogic.combinationsWithAdditionalCards(cards, drawableCards).collectFirst {
      case (drawable, _) => drawable
    }
  }
}
