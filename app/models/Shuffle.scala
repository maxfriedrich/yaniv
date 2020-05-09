package models

import scala.util.Random

object Shuffle {
  private val random = new Random()

  case class ShuffleResult(playerCards: Seq[Seq[Card]], deck: Seq[Card], pile: Pile)

  def shuffle(numPlayers: Int, playerNumCards: Int, allCards: Seq[Card], random: Random = random): ShuffleResult = {
    val shuffledDeck = random.shuffle(allCards)
    val playerCards  = (0 until numPlayers).map(i => shuffledDeck.slice(i * playerNumCards, (i + 1) * playerNumCards))
    val pileTop      = shuffledDeck.slice(numPlayers * playerNumCards, numPlayers * playerNumCards + 1).head
    val deck         = shuffledDeck.drop(numPlayers * playerNumCards + 1)

    ShuffleResult(playerCards, deck, Pile.newPile(pileTop))
  }

  case class ReshuffleResult(deck: Seq[Card], pile: Pile)

  def reshuffle(pile: Pile, random: Random = random): ReshuffleResult = {
    val newPile = Pile(pile.top, Seq.empty, Seq.empty)
    val newDeck = random.shuffle(pile.drawable.map(_.card) ++ pile.bottom)
    ReshuffleResult(newDeck, newPile)
  }

}
