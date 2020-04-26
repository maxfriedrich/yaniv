package models

import org.scalatest.FlatSpec

class ShuffleSpec extends FlatSpec {

  "Shuffle" should "not hand out duplicate cards correctly" in {
    val shuffled = Shuffle.shuffle(2)
    val allCards = shuffled.playerCards.flatten ++ shuffled.deck ++ shuffled.pile.top

    assert(shuffled.playerCards.map(_.size == NumCards).forall(_ == true))

    assert(allCards.size == Cards.Deck.size)
    assert(allCards.distinct.size == allCards.size)
  }
}
