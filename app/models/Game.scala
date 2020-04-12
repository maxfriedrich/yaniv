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

case class GameState(
    id: GameId,
    version: Int,
    players: Seq[Player],
    currentPlayer: PlayerId,
    nextAction: GameAction,
    pile: Pile,
    deck: Seq[Card],
    yaniv: Option[PlayerId]
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

  def throwCards(
      playerId: PlayerId,
      cards: Seq[Card]
  ): Either[String, GameState] = {
    if (currentPlayer != playerId)
      Left(s"Current player is $currentPlayer")
    else if (nextAction != Throw)
      Left(s"Next action is $nextAction")
    else if (yaniv.nonEmpty)
      Left(s"This game is already over")
    else if (!isValidCombination(cards))
      Left(s"Not a valid combination")
    else {
      val player = players.find(_.id == playerId).get
      if (!cards.forall(player.cards.contains))
        // TODO: this breaks for J1 apparently
        return Left("Player does not have these cards")
      val newPlayer = player.copy(cards = player.cards.filterNot(cards.toSet))
      val newPile   = pile.throwCards(cards)
      Right(
        this.copy(
          version = version + 1,
          players = players.map { p => if (p.id == playerId) newPlayer else p },
          nextAction = Draw,
          pile = newPile
        )
      )
    }
  }

  def drawCard(
      playerId: PlayerId,
      source: DrawSource
  ): Either[String, GameState] = {
    if (currentPlayer != playerId)
      Left(s"Current player is $currentPlayer")
    else if (nextAction != Draw)
      Left(s"Next action is $nextAction")
    else if (yaniv.nonEmpty)
      Left(s"This game is already over")
    else {
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
      Right(
        this.copy(
          version = version + 1,
          players = players.map { p => if (p.id == playerId) newPlayer else p },
          nextAction = Throw,
          currentPlayer = nextPlayer,
          deck = newDeck,
          pile = newPile
        )
      )
    }
  }

  def callYaniv(playerId: PlayerId): Either[String, GameState] = {
    if (currentPlayer != playerId)
      Left(s"Current player is $currentPlayer")
    else if (nextAction != Throw)
      Left(s"Next action is $nextAction")
    else if (yaniv.nonEmpty)
      Left(s"This game is already over")
    else {
      val player = players.find(_.id == playerId).get
      if (player.cards.map(_.endValue).sum > YanivPoints)
        Left(s"Calling Yaniv is only allowed with <= $YanivPoints points")
      else
        Right(this.copy(version = version + 1, yaniv = Some(playerId)))
    }
  }
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
}
