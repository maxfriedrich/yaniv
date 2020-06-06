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

  def combinationsWithAdditionalCards(cards: Seq[Card], additionalCards: Seq[Card]): Iterator[(Card, Seq[Card])] =
    for {
      additional  <- additionalCards.iterator
      choice      <- AILogic.combinations(cards)
      combination <- Seq(additional +: choice, choice :+ additional)
      if GameLogic.isValidCombination(combination) && !outsideJoker(combination)
    } yield (additional, combination)

  def doesMatchOutside(cards: Seq[Card], card: Card): Boolean =
    combinationsWithAdditionalCards(cards, Seq(card)).nonEmpty
}
