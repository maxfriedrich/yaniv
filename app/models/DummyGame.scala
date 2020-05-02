package models

import models.series.{GameSeriesConfig, GameSeriesState, PlayerInfo}

import scala.collection.mutable

object DummyGame {
  val dummyGame = "g1" -> {
    val config   = GameSeriesConfig.Default
    val shuffled = Shuffle.shuffle(2, config.gameConfig.playerNumCards)

    mutable.Buffer(
      GameSeriesState(
        config,
        id = "g1",
        version = 1,
        players = Seq(PlayerInfo("p1", "Max"), series.PlayerInfo("p2", "Pauli")),
        currentGame = Right(
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
        scores = Map("p1" -> 12, "p2" -> 3),
        scoresDiff = Map.empty
      )
    )
  }
}
