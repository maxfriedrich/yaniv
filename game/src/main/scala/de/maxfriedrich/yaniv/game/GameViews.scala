package de.maxfriedrich.yaniv.game

case class PileView(top: Seq[Card], drawable: Seq[DrawableCard], bottom: Int)

object PileView {
  def fromPile(pile: Pile): PileView =
    PileView(pile.top, pile.drawable, pile.bottom.size)
}

case class GameStateView(
    turn: Int,
    me: PlayerCards,
    otherPlayers: Seq[PlayerCardsView],
    playerOrder: Seq[PlayerId],
    currentPlayer: PlayerId,
    drawThrowPlayer: Option[PlayerId],
    nextAction: GameActionType,
    lastAction: Option[GameActionWithPlayer],
    pile: PileView,
    deck: Int,
    ending: Option[GameResult]
)

object GameStateView {
  def fromGameState(gameState: GameState, playerId: PlayerId): GameStateView = {
    val me = gameState.players.find(_.id == playerId).get
    val otherPlayers =
      gameState.players.filterNot(Set(me)).map { player =>
        if (gameState.ending.isDefined)
          FullPlayerCardsView.fromPlayerCards(player)
        else
          PartialPlayerCardsView.fromPlayerCards(player)
      }
    GameStateView(
      turn = gameState.turn,
      me = me,
      otherPlayers = otherPlayers,
      playerOrder = gameState.players.map(_.id),
      currentPlayer = gameState.currentPlayer,
      drawThrowPlayer = gameState.drawThrowPlayer,
      nextAction = gameState.nextAction,
      lastAction = gameState.lastAction,
      pile = PileView.fromPile(gameState.pile),
      deck = gameState.deck.size,
      ending = gameState.ending
    )
  }
}

trait PlayerCardsView {
  val id: PlayerId
  val numCards: Int
}

case class FullPlayerCardsView(id: PlayerId, numCards: Int, cards: Seq[Card]) extends PlayerCardsView

object FullPlayerCardsView {
  def fromPlayerCards(player: PlayerCards): FullPlayerCardsView = {
    FullPlayerCardsView(player.id, player.cards.size, player.cards)
  }
}

case class PartialPlayerCardsView(id: PlayerId, numCards: Int) extends PlayerCardsView

object PartialPlayerCardsView {
  def fromPlayerCards(player: PlayerCards): PartialPlayerCardsView = {
    PartialPlayerCardsView(player.id, player.cards.size)
  }
}
