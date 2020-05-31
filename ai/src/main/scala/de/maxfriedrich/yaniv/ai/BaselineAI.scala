package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{
  Card,
  DeckSource,
  Draw,
  DrawType,
  GameAction,
  GameLogic,
  GameStateView,
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
      Throw(findBestCombination(myCards))
    }
  }

  def playDraw(gameStateView: GameStateView): GameAction = {
    Draw(DeckSource)
  }

  def findBestCombination(cards: Seq[Card]): Seq[Card] = {
    val combinations = (1 to 5).foldLeft(Iterator(Seq.empty[Card])) {
      case (acc, n) =>
        acc ++ cards.combinations(n)
    }
    combinations.maxBy { cards => if (GameLogic.isValidCombination(cards)) cards.map(_.endValue).sum else 0 }
  }
}
