package service

import de.maxfriedrich.yaniv.game.{DeckSource, GameConfig, GameLogic, GameSeriesId, GameState, PlayerId}
import de.maxfriedrich.yaniv.game.series.{
  GameSeriesConfig,
  GameSeriesLogic,
  GameSeriesPreStartInfo,
  GameSeriesState,
  GameSeriesStateView,
  PlayerInfo
}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

class GamesConsistencySpec extends FlatSpec with BeforeAndAfterAll {

  case class DrawThrowSetup(
      gamesService: GamesStorageService,
      gameSeriesId: GameSeriesId,
      currentSeriesState: GameSeriesState,
      previousSeriesState: GameSeriesState,
      currentGameState: GameState,
      previousGameState: GameState
  )
  def setupDrawThrowGame(): DrawThrowSetup = {
    val eights = GameConfig.Default.deck.filter(_.endValue == 8)
    val gsConfig =
      GameSeriesConfig.Default.copy(gameConfig = GameConfig.Default.copy(deck = eights, playerNumCards = 1))
    val id = "test"
    val s0 = GameSeriesState.empty(gsConfig, id)
    val gs = new GamesStorageService(
      new NoNotify[GameSeriesId, GameSeriesPreStartInfo],
      new NoNotify[(GameSeriesId, PlayerId), GameSeriesStateView]
    )
    val result = for {
      _  <- gs.create(s0)
      s1 <- GameSeriesLogic.addPlayer(s0, PlayerInfo("P1", "p1"))
      _  <- gs.update(id, s1)
      s2 <- GameSeriesLogic.addPlayer(s1, PlayerInfo("P2", "p2"))
      _  <- gs.update(id, s2)
      s3 <- GameSeriesLogic.startSeries(s2)
      _  <- gs.update(id, s3)
      g1 <- s3.currentGame.toRight("game did not start")
      g2 <- GameLogic.throwCards(g1, g1.currentPlayer, g1.players.find(_.id == g1.currentPlayer).get.cards)
      s4 <- GameSeriesLogic.updateGameState(s3, g2)
      _  <- gs.update(id, s4)
      g3 <- GameLogic.drawCard(g2, g2.currentPlayer, DeckSource)
      s5 <- GameSeriesLogic.updateGameState(s4, g3)
      _  <- gs.update(id, s5)
      g4 <- GameLogic.throwCards(g3, g3.currentPlayer, g3.players.find(_.id == g3.currentPlayer).get.cards)
      s6 <- GameSeriesLogic.updateGameState(s5, g4)
      _  <- gs.update(id, s6)
    } yield DrawThrowSetup(gs, id, s6, s5, g4, g3)
    result.right.get
  }

  "Games service" should "accept new game state if version is set correctly (updating current series state)" in {
    val setup           = setupDrawThrowGame()
    val drawThrowPlayer = setup.currentGameState.drawThrowPlayer.get
    val result = for {
      // draw-throw on the previous game state
      invalidState <- GameLogic.drawThrowCard(
        setup.previousGameState,
        drawThrowPlayer,
        setup.previousGameState.players.find(_.id == drawThrowPlayer).get.drawThrowable.get
      )
      invalidSeriesState <- GameSeriesLogic.updateGameState(setup.currentSeriesState, invalidState)
      _                  <- setup.gamesService.update(setup.gameSeriesId, invalidSeriesState)
    } yield "should not happen!" // caught by GamesService
    assert(result.isLeft && result.left.get.contains("too old"))
  }

  // this is caught by GameSeriesLogic, so it should not be part of this spec
  "Games service" should "accept new game state if version is set correctly (updating previous series state)" in {
    val setup           = setupDrawThrowGame()
    val drawThrowPlayer = setup.currentGameState.drawThrowPlayer.get
    val result = for {
      // draw-throw on the previous game state
      invalidState <- GameLogic.drawThrowCard(
        setup.previousGameState,
        drawThrowPlayer,
        setup.previousGameState.players.find(_.id == drawThrowPlayer).get.drawThrowable.get
      )
      invalidSeriesState <- GameSeriesLogic.updateGameState(setup.previousSeriesState, invalidState)
      _                  <- setup.gamesService.update(setup.gameSeriesId, invalidSeriesState)
    } yield "should not happen!" // caught by GameSeriesLogic
    assert(result.isLeft && result.left.get.contains("too old"))
  }
}
