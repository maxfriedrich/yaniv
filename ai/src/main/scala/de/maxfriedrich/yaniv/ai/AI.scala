package de.maxfriedrich.yaniv.ai

import de.maxfriedrich.yaniv.game.{GameAction, GameStateView}

// Playing only a single game!
trait AI[S] {
  def update(version: Int, gameStateView: GameStateView): AI[S]
  def playTurn(gameStateView: GameStateView): GameAction
  def playDrawThrow(gameStateView: GameStateView): Option[GameAction]
}
