package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{Card, GameLogic, Joker}

object AILogic {
  def combinations(cards: Seq[Card]): Iterator[Seq[Card]] =
    (1 to 5).foldLeft(Iterator[Seq[Card]]()) {
      case (acc, n) =>
        acc ++ cards.combinations(n).flatMap(_.permutations)
    }

  def outsideJoker(cards: Seq[Card]): Boolean = (cards.head, cards.last) match {
    case (Joker(_), _) => true
    case (_, Joker(_)) => true
    case _             => false
  }

  def bestCombination(cards: Seq[Card], allowOutsideJokers: Boolean = false): Seq[Card] = {
    combinations(cards)
      .filterNot { combination => if (allowOutsideJokers) outsideJoker(combination) else false }
      .maxBy { cards => if (GameLogic.isValidCombination(cards)) cards.map(_.endValue).sum else 0 }
  }

  def combinationsWithDrawable(cards: Seq[Card], drawableCards: Seq[Card]): Iterator[(Card, Seq[Card])] =
    for {
      drawable    <- drawableCards.iterator
      hand        <- AILogic.combinations(cards)
      combination <- Seq(drawable +: hand, hand :+ drawable)
      if GameLogic.isValidCombination(combination) && !outsideJoker(combination)
    } yield (drawable, combination)
}
