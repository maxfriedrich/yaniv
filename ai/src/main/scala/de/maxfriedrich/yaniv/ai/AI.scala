package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{GameAction, GameStateView}

trait AI {
  def update(gameStateView: GameStateView): Unit
  def play(gameStateView: GameStateView): GameAction
}
