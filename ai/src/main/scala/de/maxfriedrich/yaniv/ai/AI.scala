package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{GameAction, GameStateView}

trait AI {
  def update(gameStateView: GameStateView): Unit
  def playTurn(gameStateView: GameStateView): GameAction
  def playDrawThrow(gameStateView: GameStateView): Option[GameAction]
}
