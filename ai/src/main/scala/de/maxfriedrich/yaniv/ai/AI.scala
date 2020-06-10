package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{GameAction, GameStateView}

// Playing only a single game!
trait AI {
  def update(version: Int, gameStateView: GameStateView): Unit
  def playTurn(gameStateView: GameStateView): GameAction
  def playDrawThrow(gameStateView: GameStateView): Option[GameAction]
}
