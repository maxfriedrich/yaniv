package de.maxfriedrich.yaniv.game

import de.maxfriedrich.yaniv.game.series.PlayerInfo

sealed trait GameAction
case object Throw extends GameAction
case object Draw  extends GameAction

sealed trait LastGameAction
case object Started                  extends LastGameAction
case class Drawn(source: DrawSource) extends LastGameAction
case class Thrown(cards: Seq[Card])  extends LastGameAction
case class DrawThrown(card: Card)    extends LastGameAction

case class PlayerCards(id: PlayerId, cards: Seq[Card], drawThrowable: Option[Card])

sealed trait DrawSource
case object DeckSource            extends DrawSource
case class PileSource(card: Card) extends DrawSource

sealed trait GameEnding {
  val winner: PlayerId
}
case class Yaniv(winner: PlayerId, points: Int)                                     extends GameEnding
case class Asaf(caller: PlayerId, points: Int, winner: PlayerId, winnerPoints: Int) extends GameEnding
case class EmptyHand(winner: PlayerId)                                              extends GameEnding

case class GameResult(ending: GameEnding, points: Map[PlayerId, Int])

case class GameState(
    config: GameConfig,
    version: Int = 0,
    players: Seq[PlayerCards],
    currentPlayer: PlayerId,
    nextAction: GameAction,
    lastAction: LastGameAction,
    pile: Pile,
    deck: Seq[Card],
    ending: Option[GameResult]
) {
  // copy forcing a version update but dumb otherwise (use GameLogic methods instead!)
  // TODO: not very nice to mix logic into the case class :/
  private[game] def copy(
      players: Seq[PlayerCards] = players,
      currentPlayer: PlayerId = currentPlayer,
      nextAction: GameAction = nextAction,
      lastAction: LastGameAction = lastAction,
      pile: Pile = pile,
      deck: Seq[Card] = deck,
      ending: Option[GameResult] = ending
  ): GameState =
    GameState(
      config = config,
      version = version + 1,
      players = players,
      currentPlayer = currentPlayer,
      nextAction = nextAction,
      lastAction = lastAction,
      pile = pile,
      deck = deck,
      ending = ending
    )

}

object GameState {
  def newGame(config: GameConfig, players: Seq[PlayerInfo], startingPlayer: Option[PlayerId] = None): GameState = {
    val shuffled = Shuffle.shuffle(players.size, config.playerNumCards, config.deck)
    GameState(
      config = config,
      players = players.zip(shuffled.playerCards).map {
        case (player, cards) => PlayerCards(id = player.id, cards = cards, drawThrowable = None)
      },
      currentPlayer = startingPlayer.getOrElse(players.head.id),
      nextAction = Throw,
      lastAction = Started,
      pile = shuffled.pile,
      deck = shuffled.deck,
      ending = None
    )
  }
}
