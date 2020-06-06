package de.maxfriedrich.yaniv.ai

import org.scalatest.FlatSpec
import CardsHelper.C

class AILogicSpec extends FlatSpec {
  val h5    = C("H5")
  val h3    = C("H3")
  val d4    = C("D4")
  val d3    = C("D3")
  val j1    = C("J1")
  val cards = Seq(h5, h3, d4, d3, j1)

  "Combinations" should "find combinations in all orders" in {
    val combinations = AILogic.combinations(cards).toSeq
    assert(!combinations.contains(Seq.empty))
    assert(combinations.contains(Seq(h3, d3)))
    assert(combinations.contains(Seq(d3, h3)))
    assert(combinations.contains(Seq(d3, j1, h5, h3, d4)))
    // size 1:   5 combinations *   1 permutation
    // size 2:  10 combinations *   2 permutations
    // size 3:  10 combinations *   6 permutations
    // size 4:   5 combinations *  24 permutations
    // size 5:   1 combination  * 120 permutations
    // total:  325
    assert(combinations.length == 325)
  }

  "Best combination" should "find combination with joker" in {
    assert(AILogic.bestCombination(cards) == Seq(h3, j1, h5))
  }

  "Combinations with drawable" should "find valid combinations" in {
    assert(AILogic.combinationsWithAdditionalCards(Seq(h3, j1, d4, d3), Seq(h5)).toSeq == Seq((h5, Seq(h3, j1, h5))))
  }
}
