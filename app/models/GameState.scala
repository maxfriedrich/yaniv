package models

sealed trait GameAction
object Throw extends GameAction {
  override def toString: PlayerId = "throw"
}
object Draw extends GameAction {
  override def toString: PlayerId = "draw"
}

case class PlayerCards(id: PlayerId, cards: Seq[Card])

sealed trait DrawSource
object DeckSource                 extends DrawSource
case class PileSource(card: Card) extends DrawSource

sealed trait GameEnding
case class Yaniv(caller: PlayerId)                  extends GameEnding
case class Asaf(caller: PlayerId, winner: PlayerId) extends GameEnding

case class GameResult(ending: GameEnding, points: Map[PlayerId, Int])

case class GameState(
    players: Seq[PlayerCards],
    currentPlayer: PlayerId,
    nextAction: GameAction,
    pile: Pile,
    deck: Seq[Card],
    ending: Option[GameResult]
)

object GameState {
  def newGame(id: GameSeriesId, players: Seq[PlayerCards]): GameState = {
    val shuffled = Shuffle.shuffle(players.size)
    GameState(
      players.zip(shuffled.playerCards).map { case (player, cards) => player.copy(cards = cards) },
      players.head.id,
      Throw,
      shuffled.pile,
      shuffled.deck,
      None
    )
  }
}
