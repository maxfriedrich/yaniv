package models

case class PileView(top: Seq[Card], drawable: Seq[DrawableCard], bottom: Int)

object PileView {
  def fromPile(pile: Pile): PileView =
    PileView(pile.top, pile.drawable, pile.bottom.size)
}

case class GameStateView(
    me: PlayerCards,
    otherPlayers: Seq[PlayerCardsView],
    currentPlayer: PlayerId,
    nextAction: GameAction,
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
      me,
      otherPlayers,
      gameState.currentPlayer,
      gameState.nextAction,
      PileView.fromPile(gameState.pile),
      gameState.deck.size,
      gameState.ending
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

case class GameSeriesStateView(
    id: GameSeriesId,
    version: Int,
    timestamp: String,
    players: Seq[PlayerInfo],
    gameState: Option[GameStateView],
    scores: Map[PlayerId, Int]
)

object GameSeriesStateView {
  def fromGameSeriesState(gameSeries: GameSeriesState, playerId: PlayerId): GameSeriesStateView = {
    GameSeriesStateView(
      id = gameSeries.id,
      version = gameSeries.version,
      timestamp = gameSeries.timestamp,
      players = gameSeries.players,
      gameState = gameSeries.gameState.map(gs => GameStateView.fromGameState(gs, playerId)),
      scores = gameSeries.scores
    )
  }
}
