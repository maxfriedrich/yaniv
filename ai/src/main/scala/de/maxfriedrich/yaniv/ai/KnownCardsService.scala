package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{
  Card,
  Draw,
  DrawThrow,
  GameActionWithPlayer,
  PileSource,
  PlayerCardsView,
  PlayerId,
  Throw
}

import scala.collection.mutable

class KnownCardsService(players: Seq[PlayerId]) {
  private val playerIds = players
  private val known     = mutable.Map.empty[PlayerId, Set[Card]]

  reset()

  def reset(): Unit = {
    playerIds.foreach { playerId => known += playerId -> Set.empty }
  }

  def update(players: Seq[PlayerCardsView], lastAction: Option[GameActionWithPlayer]): Unit =
    for {
      action <- lastAction
      playerId    = action.player
      playerCards = known(action.player)
    } yield {
      val newCards = action.gameAction match {
        case Draw(PileSource(card)) => Some(playerCards ++ Set(card))
        case Throw(cards)           => Some(playerCards -- cards)
        case DrawThrow(card)        => Some(playerCards -- Set(card))
        case _                      => None
      }
      newCards.foreach(cards => known += playerId -> cards)
    }

  def get(playerId: PlayerId): Set[Card] = known.getOrElse(playerId, Set.empty)
}
