package models

import models.series.{GameIsRunning, GameSeriesConfig, GameSeriesLogic, GameSeriesState, PlayerInfo, WaitingForNextGame}

import scala.collection.mutable

object DummyGame {
  val dummyGame = "g1" -> {
    val config   = GameSeriesConfig.Default
    val shuffled = Shuffle.shuffle(2, config.gameConfig.playerNumCards, config.gameConfig.deck)

    mutable.Buffer(
      GameSeriesState(
        config,
        id = "g1",
        version = 1,
        players = Seq(PlayerInfo("p1", "Max"), series.PlayerInfo("p2", "Pauli")),
        state = GameIsRunning,
        currentGame = Some(
          GameState(
            config = config.gameConfig,
            players = Seq(
              PlayerCards("p1", shuffled.playerCards(0), None),
              PlayerCards("p2", shuffled.playerCards(1), None)
            ),
            currentPlayer = "p1",
            nextAction = Throw,
            pile = shuffled.pile,
            deck = shuffled.deck,
            ending = None
          )
        ),
        lastWinner = None,
        scores = Map("p1" -> 12, "p2" -> 3),
        scoresDiff = Map.empty
      )
    )
  }

  // dummy game for testing draw-throw in the frontend: only one player, only eights in the deck
  val drawThrowTest = "g2" -> {
    val eights = Cards.Deck.filter(_.endValue == 8)
    val config = GameSeriesConfig.Default
      .copy(gameConfig = GameSeriesConfig.Default.gameConfig.copy(deck = eights, playerNumCards = 2))

    val empty = GameSeriesState.empty(config, "g2")

    val state = (for {
      withPlayer <- GameSeriesLogic.addPlayer(empty, PlayerInfo("p1", "Max"))
      started    <- GameSeriesLogic.startSeries(withPlayer)
    } yield started).toSeq

    mutable.Buffer(
      state: _*
    )
  }

  val betweenGames = "g3" -> {
    val config   = GameSeriesConfig.Default
    val shuffled = Shuffle.shuffle(2, config.gameConfig.playerNumCards, config.gameConfig.deck)
    mutable.Buffer(
      GameSeriesState(
        config,
        id = "g3",
        version = 1,
        players = Seq(PlayerInfo("p1", "Max"), series.PlayerInfo("p2", "Pauli")),
        state = WaitingForNextGame(Set("p1")),
        currentGame = Some(
          GameState(
            config = config.gameConfig,
            players = Seq(
              PlayerCards("p1", Seq.empty, None),
              PlayerCards("p2", Seq.empty, None)
            ),
            currentPlayer = "p1",
            nextAction = Throw,
            pile = shuffled.pile,
            deck = shuffled.deck,
            ending = Some(GameResult(Yaniv("p1", 2), Map("p1" -> 2, "p2" -> 4)))
          )
        ),
        lastWinner = None,
        scores = Map("p1"     -> 25, "p2"          -> 15),
        scoresDiff = Map("p1" -> Seq(2, -25), "p2" -> Seq(4))
      )
    )
  }
}
