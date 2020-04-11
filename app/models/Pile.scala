package models

case class Pile(
    top: Seq[Card],
    drawable: Seq[DrawableCard],
    bottom: Seq[Card]
) {
  import Pile._

  def throwCards(cards: Seq[Card]): Pile =
    this.copy(
      top = cards,
      drawable = makeDrawable(top),
      bottom = drawable.map(_.card) ++ bottom
    )

  def drawCard(card: Card): Pile =
    this.copy(drawable = drawable.filterNot(_.card == card))
}

object Pile {
  def newPile(initialCard: Card): Pile = {
    Pile(top = Seq(initialCard), Seq.empty, Seq.empty)
  }

  def makeDrawable(cards: Seq[Card]): Seq[DrawableCard] = {
    if (cards.isEmpty)
      Seq.empty
    else
      cards.zipWithIndex.map {
        case (c, i) =>
          DrawableCard(
            c,
            Set(cards.indices.head, cards.indices.last).contains(i)
          )
      }
  }
}
