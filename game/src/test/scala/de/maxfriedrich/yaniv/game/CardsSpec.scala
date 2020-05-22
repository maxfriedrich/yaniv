package de.maxfriedrich.yaniv.game

import de.maxfriedrich.yaniv.game.CardsHelper.C
import org.scalatest.FlatSpec

class CardsSpec extends FlatSpec {

  val h4 = C("H4")
  val j1 = C("J1")
  val d4 = C("D4")
  val h3 = C("D3")

  "Card" should "parse cards from string correctly" in {
    val parsed = Card.fromString("H4")
    assert(parsed.get == RegularCard(Hearts, CardValue("4", 4, 4)))

    val parsed2 = Card.fromString("H1")
    assert(parsed2.isEmpty)
  }

  "Pile" should "behave correctly" in {
    val p  = Pile(Seq.empty, Seq.empty, Seq.empty)
    val p2 = p.throwCards(Seq(h4, j1, d4))
    val p3 = p2.throwCards(Seq(h3))
    assert(p3.top == Seq(h3))
    assert(p3.drawable == Seq(DrawableCard(h4, true), DrawableCard(j1, false), DrawableCard(d4, true)))
  }
}
