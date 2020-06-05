package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{
  Card,
  DeckSource,
  Draw,
  DrawType,
  GameAction,
  GameStateView,
  PileSource,
  Throw,
  ThrowType,
  Yaniv
}

class BaselineAI extends AI {
  import BaselineAI._

  override def update(gameStateView: GameStateView): Unit = ()

  override def play(gameStateView: GameStateView): GameAction = gameStateView.nextAction match {
    case ThrowType => playThrow(gameStateView) // can't draw-throw yet!
    case DrawType  => playDraw(gameStateView)
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
    cardToDrawFromPile(gameStateView.me.cards, gameStateView.pile.drawable.map(_.card)) match {
      case Some(card) => Draw(PileSource(card))
      case _          => Draw(DeckSource)
    }
  }

  def cardToDrawFromPile(cards: Seq[Card], drawableCards: Seq[Card]): Option[Card] = {
    AILogic.combinationsWithDrawable(cards, drawableCards).collectFirst {
      case (drawable, combination) => drawable
    }
  }
}
