package models

import Game._

import scala.util.Random

object Game {
  Random.setSeed(1L)
  type GameId   = String
  type PlayerId = String
  val YanivPoints = 5
}

sealed trait GameAction
object Throw extends GameAction {
  override def toString: PlayerId = "throw"
}
object Draw extends GameAction {
  override def toString: PlayerId = "draw"
}

case class Player(id: PlayerId, name: String, cards: Seq[Card])

sealed trait DrawSource
object DeckSource                 extends DrawSource
case class PileSource(card: Card) extends DrawSource

sealed trait GameEnding
case class Yaniv(caller: PlayerId)                  extends GameEnding
case class Asaf(caller: PlayerId, winner: PlayerId) extends GameEnding

case class GameResult(ending: GameEnding, points: Map[PlayerId, Int])

case class GameState(
    id: GameId,
    version: Int,
    players: Seq[Player],
    currentPlayer: PlayerId,
    nextAction: GameAction,
    pile: Pile,
    deck: Seq[Card],
    ending: Option[GameResult]
) {

  import GameState._

  val nextPlayer: PlayerId = {
    val order        = players.sortBy(_.id)
    val currentIndex = order.indexWhere(_.id == currentPlayer)
    if (currentIndex + 1 == players.size)
      order.head.id
    else
      order(currentIndex + 1).id
  }

  private def validateGameState(playerId: PlayerId, expectedAction: GameAction): Either[String, GameState] = {
    if (currentPlayer != playerId)
      Left(s"Current player is $currentPlayer")
    else if (nextAction != expectedAction)
      Left(s"Next action is $nextAction")
    else if (ending.nonEmpty)
      Left(s"This game is already over")
    else
      Right(this)
  }

  def throwCards(playerId: PlayerId, cards: Seq[Card]): Either[String, GameState] = {
    for {
      _ <- validateGameState(playerId, Throw)
      _ <- if (isValidCombination(cards)) Right(()) else Left(s"Not a valid combination")
      player = players.find(_.id == playerId).get
      _ <- if (cards.forall(player.cards.contains)) Right(()) else Left("Player does not have these cards")
    } yield {
      val newPlayer = player.copy(cards = player.cards.filterNot(cards.toSet))
      val newPile   = pile.throwCards(cards)
      this.copy(
        version = version + 1,
        players = players.map { p => if (p.id == playerId) newPlayer else p },
        nextAction = Draw,
        pile = newPile
      )
    }
  }

  def drawCard(playerId: PlayerId, source: DrawSource): Either[String, GameState] = {
    for {
      _ <- validateGameState(playerId, Draw)
    } yield {
      val (newCard, newDeck, newPile) = source match {
        case DeckSource => (deck.head, deck.drop(1), pile)
        case PileSource(card) =>
          if (!pile.drawable.filter(_.drawable).map(_.card).contains(card))
            return Left(s"Card ${card.id} is not drawable")
          else
            (card, deck, pile.drawCard(card))
      }

      val player    = players.find(_.id == playerId).get
      val newPlayer = player.copy(cards = player.cards ++ Seq(newCard))
      this.copy(
        version = version + 1,
        players = players.map { p => if (p.id == playerId) newPlayer else p },
        nextAction = Throw,
        currentPlayer = nextPlayer,
        deck = newDeck,
        pile = newPile
      )
    }
  }

  def callYaniv(playerId: PlayerId): Either[String, GameState] =
    for {
      _ <- validateGameState(playerId, Throw)
      player       = players.find(_.id == playerId).get
      playerPoints = player.cards.map(_.endValue).sum
      _ <- if (playerPoints <= YanivPoints) Right(())
      else Left(s"Calling Yaniv is only allowed with <= $YanivPoints points")
    } yield this.copy(version = version + 1, ending = Some(finishGame(this, playerId)))
}

object GameState {
  def newGame(id: GameId, players: Seq[Player]): GameState = {
    val shuffledDeck    = Random.shuffle(Cards.Deck)
    val (pileTop, deck) = (shuffledDeck.head, shuffledDeck.drop(1))
    GameState(
      id,
      1,
      players,
      players.head.id,
      Throw,
      Pile.newPile(pileTop),
      deck,
      None
    )
  }

  def isValidCombination(cards: Seq[Card]): Boolean = {
    if (cards.size == 1) {
      true
    } else {
      val jokers = cards.collect { case j: Joker       => j }
      val others = cards.collect { case o: RegularCard => o }

      if (jokers.size == 3 && others.isEmpty)
        true
      else if (others.map(_.value.position).distinct.size == 1)
        true
      else if (cards.size >= 3 && others.map(_.suit).distinct.size == 1)
        checkStreet(cards)
      else
        false
    }
  }

  private def checkStreet(cards: Seq[Card]): Boolean = {
    val withoutLeftJokers = cards.dropWhile { c => c.isInstanceOf[Joker] }
    withoutLeftJokers
      .drop(1)
      .foldLeft(
        (
          true,
          withoutLeftJokers.head
            .asInstanceOf[RegularCard]
            .value
            .position
        )
      ) { (acc, card) =>
        card match {
          case Joker(_) => (acc._1, acc._2 + 1)
          case c: RegularCard if acc._2 + 1 == c.value.position =>
            (acc._1, acc._2 + 1)
          case _ => (false, acc._2 + 1)
        }
      }
      ._1
  }

  private def finishGame(gameState: GameState, yanivCaller: PlayerId): GameResult = {
    val points           = gameState.players.map(p => p.id -> p.cards.map(c => c.endValue).sum).toMap
    val minPointsPlayers = points.groupBy(_._2).toSeq.minBy(_._1)._2.keys.toSeq

    val ending =
      if (minPointsPlayers.size == 1 && minPointsPlayers.head == yanivCaller)
        Yaniv(yanivCaller)
      else
        Asaf(yanivCaller, minPointsPlayers.filter(_ != yanivCaller).head)

    GameResult(ending, points)
  }

}
