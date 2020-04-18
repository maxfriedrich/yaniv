package models

import scala.util.Random

object Shuffle {
  Random.setSeed(1L)

  case class ShuffleResult(playerCards: Seq[Seq[Card]], deck: Seq[Card], pile: Pile)

  def shuffle(numPlayers: Int): ShuffleResult = {
    val shuffledDeck = Random.shuffle(Cards.Deck)
    val playerCards  = (0 until numPlayers).map(i => shuffledDeck.slice(i, i + NumCards))
    val pileTop      = shuffledDeck.slice(numPlayers * NumCards, numPlayers * NumCards + 1).head
    val deck         = shuffledDeck.drop(numPlayers * NumCards + 1)

    ShuffleResult(playerCards, deck, Pile.newPile(pileTop))
  }
}
