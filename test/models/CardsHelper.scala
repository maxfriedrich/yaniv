package models

object CardsHelper {
  // Use `C("H4")` as a shorthand for `Card.fromString("C4").get` in tests
  val C: Map[String, Card] = Cards.Deck.map(c => c.id -> c).toMap
}
