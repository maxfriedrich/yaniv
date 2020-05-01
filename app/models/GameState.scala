package models

import models.series.PlayerInfo

sealed trait GameAction
case object Throw extends GameAction
case object Draw  extends GameAction

case class PlayerCards(id: PlayerId, cards: Seq[Card], drawThrowable: Option[Card])

sealed trait DrawSource
case object DeckSource            extends DrawSource
case class PileSource(card: Card) extends DrawSource

sealed trait GameEnding
case class Yaniv(caller: PlayerId)                  extends GameEnding
case class Asaf(caller: PlayerId, winner: PlayerId) extends GameEnding

case class GameResult(ending: GameEnding, points: Map[PlayerId, Int])

case class GameState(
    config: GameConfig,
    players: Seq[PlayerCards],
    currentPlayer: PlayerId,
    nextAction: GameAction,
    pile: Pile,
    deck: Seq[Card],
    ending: Option[GameResult]
)

object GameState {
  def newGame(config: GameConfig, players: Seq[PlayerInfo]): GameState = {
    val shuffled = Shuffle.shuffle(players.size, config.playerNumCards)
    GameState(
      config = config,
      players = players.zip(shuffled.playerCards).map {
        case (player, cards) => PlayerCards(id = player.id, cards = cards, drawThrowable = None)
      },
      currentPlayer = players.head.id,
      nextAction = Throw,
      pile = shuffled.pile,
      deck = shuffled.deck,
      ending = None
    )
  }
}
