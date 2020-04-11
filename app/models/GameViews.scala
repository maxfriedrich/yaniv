package models

import models.Game.{GameId, PlayerId}

case class PileView(top: Seq[Card], drawable: Seq[DrawableCard], bottom: Int)

object PileView {
  def fromPile(pile: Pile): PileView =
    PileView(pile.top, pile.drawable, pile.bottom.size)
}

case class GameStateView(
    id: GameId,
    version: Int,
    me: Player,
    otherPlayers: Seq[PlayerView],
    currentPlayer: PlayerId,
    nextAction: GameAction,
    pile: PileView,
    deck: Int,
    yaniv: Option[PlayerId]
)

object GameStateView {
  def fromGameState(gameState: GameState, playerId: PlayerId): GameStateView = {
    val me = gameState.players.find(_.id == playerId).get
    val otherPlayers =
      gameState.players.filterNot(Set(me)).map(PlayerView.fromPlayer)
    GameStateView(
      gameState.id,
      gameState.version,
      me,
      otherPlayers,
      gameState.currentPlayer,
      gameState.nextAction,
      PileView.fromPile(gameState.pile),
      gameState.deck.size,
      gameState.yaniv
    )
  }
}

case class PlayerView(id: PlayerId, name: String, cards: Int)

object PlayerView {
  def fromPlayer(player: Player): PlayerView = {
    PlayerView(player.id, player.name, player.cards.size)
  }
}
