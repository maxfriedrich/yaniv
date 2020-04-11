package models

import models.Cards.{CardValues, Suits}

import scala.util.Random

sealed trait Suit {
  val id: String
  val icon: String
}

object Diamonds extends Suit {
  val id = "D"
  val icon = "♦"
}

object Cross extends Suit {
  val id = "C"
  val icon = "♣"
}

object Hearts extends Suit {
  val id = "H"
  val icon = "♥"
}

object Spades extends Suit {
  val id = "S"
  val icon = "♠"
}

case class CardValue(id: String, position: Int, endValue: Int)

trait Card {
  val id: String
  val gameString: String
  val endValue: Int
}

object Card {
  def fromString(id: String): Option[Card] = (id(0), id(1)) match {
    case ('J', '1') | ('J', '2') | ('J', '3') => Some(Joker(id(1).toInt))
    case (s, v) =>
      for {
        suit <- Suits.find(_.id == s.toString)
        value <- CardValues.find(_.id == v.toString)
      } yield RegularCard(suit, value)
  }
}

case class RegularCard(suit: Suit, value: CardValue) extends Card {
  val id = suit.id + value.id
  val gameString = suit.icon + value.id
  val endValue = value.endValue
}

case class Joker(number: Int) extends Card {
  val id = s"J$number"
  val gameString = "J"
  val endValue = 0

}

case class DrawableCard(card: Card, drawable: Boolean)

object Cards {
  val Suits = Seq(Diamonds, Cross, Hearts, Spades)
  val CardValues = Seq(
    CardValue("A", 1, 1),
    CardValue("2", 2, 2),
    CardValue("3", 3, 3),
    CardValue("4", 4, 4),
    CardValue("5", 5, 5),
    CardValue("6", 6, 6),
    CardValue("7", 7, 7),
    CardValue("8", 8, 8),
    CardValue("9", 9, 9),
    CardValue("10", 10, 10),
    CardValue("J", 11, 10),
    CardValue("Q", 12, 10),
    CardValue("K", 13, 10)
  )

  val Deck = (for {
    suit <- Suits
    value <- CardValues
  } yield RegularCard(suit, value)) ++ Seq(Joker(1), Joker(2), Joker(3))

  Random.setSeed(1)
  def shuffledDeck(): Seq[Card] = Random.shuffle(Deck)

}
