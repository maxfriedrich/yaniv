package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{Card, GameLogic, Joker}

object AILogic {
  def combinations(cards: Seq[Card]): Iterator[Seq[Card]] =
    (1 to 5).foldLeft(Iterator[Seq[Card]]()) {
      case (acc, n) =>
        acc ++ cards.combinations(n).flatMap(_.permutations)
    }

  def outsideJoker(cards: Seq[Card]): Boolean =
    outsideCards(cards, {
      case Joker(_) => true
      case _        => false
    })

  def outsideCards(cards: Seq[Card], outsideMatcher: Card => Boolean): Boolean = (cards.head, cards.last) match {
    case (c, _) if outsideMatcher(c) => true
    case (_, c) if outsideMatcher(c) => true
    case _                           => false
  }

  def bestCombination(cards: Seq[Card], avoidOutside: Set[Card] = Set.empty): Seq[Card] = {
    val sortedCombinations = combinations(cards)
      .filterNot(outsideJoker)
      .toSeq
      .sortBy { cards => if (GameLogic.isValidCombination(cards)) -cards.map(_.endValue).sum else 0 }
    val bestAvoidingOutside = sortedCombinations.collectFirst {
      case combination if !outsideCards(combination, avoidOutside.contains) => combination
    }
    bestAvoidingOutside.getOrElse(sortedCombinations.head)
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
