package de.maxfriedrich.yaniv.game

import org.scalatest.FlatSpec

class ShuffleSpec extends FlatSpec {

  "Shuffle" should "not hand out duplicate cards" in {
    val shuffled = Shuffle.shuffle(numPlayers = 2, playerNumCards = 5, Cards.Deck)
    val allCards = shuffled.playerCards.flatten ++ shuffled.deck ++ shuffled.pile.top

    assert(shuffled.playerCards.map(_.size == 5).forall(_ == true))

    assert(allCards.size == Cards.Deck.size)
    assert(allCards.distinct.size == allCards.size)
  }
}
