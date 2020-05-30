package de.maxfriedrich.yaniv.game

import de.maxfriedrich.yaniv.game.series.PlayerInfo

sealed trait GameActionType
case object ThrowType extends GameActionType
case object DrawType  extends GameActionType

sealed trait GameAction
case object Start                   extends GameAction
case class Draw(source: DrawSource) extends GameAction
case class Throw(cards: Seq[Card])  extends GameAction
case class DrawThrow(card: Card)    extends GameAction
case object Yaniv                   extends GameAction

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
    nextAction: GameActionType,
    lastAction: GameAction,
    pile: Pile,
    deck: Seq[Card],
    ending: Option[GameResult]
) {
  // copy forcing a version update but dumb otherwise (use GameLogic methods instead!)
  // TODO: not very nice to mix logic into the case class :/
  private[game] def copy(
      players: Seq[PlayerCards] = players,
      currentPlayer: PlayerId = currentPlayer,
      nextAction: GameActionType = nextAction,
      lastAction: GameAction = lastAction,
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
      nextAction = ThrowType,
      lastAction = Start,
      pile = shuffled.pile,
      deck = shuffled.deck,
      ending = None
    )
  }
}
