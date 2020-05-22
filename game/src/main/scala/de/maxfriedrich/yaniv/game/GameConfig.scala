package de.maxfriedrich.yaniv.game

case class GameConfig(deck: Seq[Card], playerNumCards: Int, yanivMaxPoints: Int, asafPenalty: Int)

object GameConfig {
  val Default: GameConfig = GameConfig(Cards.Deck, playerNumCards = 5, yanivMaxPoints = 5, asafPenalty = 30)
}
