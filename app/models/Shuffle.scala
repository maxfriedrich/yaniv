package models

import scala.util.Random

object Shuffle {
  Random.setSeed(1L)

  case class ShuffleResult(playerCards: Seq[Seq[Card]], deck: Seq[Card], pile: Pile)

  def shuffle(numPlayers: Int, playerNumCards: Int): ShuffleResult = {
    val shuffledDeck = Random.shuffle(Cards.Deck)
    val playerCards  = (0 until numPlayers).map(i => shuffledDeck.slice(i * playerNumCards, (i + 1) * playerNumCards))
    val pileTop      = shuffledDeck.slice(numPlayers * playerNumCards, numPlayers * playerNumCards + 1).head
    val deck         = shuffledDeck.drop(numPlayers * playerNumCards + 1)

    ShuffleResult(playerCards, deck, Pile.newPile(pileTop))
  }

  case class ReshuffleResult(deck: Seq[Card], pile: Pile)

  def reshuffle(pile: Pile): ReshuffleResult = {
    val newPile = Pile(pile.top, Seq.empty, Seq.empty)
    val newDeck = Random.shuffle(pile.drawable.map(_.card) ++ pile.bottom)
    ReshuffleResult(newDeck, newPile)
  }

}
