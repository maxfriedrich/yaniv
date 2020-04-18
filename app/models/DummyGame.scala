package models

import scala.collection.mutable

object DummyGame {
  val dummyGame = "g1" -> {
    val shuffled = Shuffle.shuffle(2)

    mutable.Buffer(
      GameSeriesState(
        id = "g1",
        version = 1,
        players = Seq(PlayerInfo("p1", "Max"), PlayerInfo("p2", "Pauli")),
        gameState = Some(
          GameState(
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
        scores = Map("p1" -> 12, "p2" -> 3)
      )
    )
  }
}