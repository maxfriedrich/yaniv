package models.series

import models.{Cards, PlayerCards}
import models.CardsHelper.C
import org.scalatest.FlatSpec

class GameSeriesLogicSpec extends FlatSpec {

  val twoKings                     = Set(C("DK"), C("HK"))
  val twoKingsRule: PointRule      = PointRule(Hand(twoKings), 0)
  val seventyPointsRule: PointRule = PointRule(Score(70), 1)

  "Point rules logic" should "apply a card rule correctly" in {

    val notOnlyTwoKings =
      GameSeriesLogic.applyRule(twoKingsRule, Set(C("DK"), C("HK"), C("J2")), 99, excludedFromScoreRules = false)
    assert(notOnlyTwoKings == 99)

    val onlyTwoKings =
      GameSeriesLogic.applyRule(twoKingsRule, Set(C("HK"), C("DK")), 99, excludedFromScoreRules = false)
    assert(onlyTwoKings == 0)
  }

  it should "apply a points rule correctly" in {
    assert(GameSeriesLogic.applyRule(seventyPointsRule, Set.empty, 71, excludedFromScoreRules = false) == 71)
    assert(GameSeriesLogic.applyRule(seventyPointsRule, Cards.Jokers.toSet, 70, excludedFromScoreRules = false) == 1)
  }

  it should "apply multiple point rules in the right order" in {
    val players   = Seq(PlayerCards("a", Seq(C("DK"), C("HK")), None), PlayerCards("b", Seq(C("J1")), None))
    val scores    = Map("a" -> 70, "b" -> 99)
    val newScores = GameSeriesLogic.applyPointRules(Seq(twoKingsRule, seventyPointsRule), players, scores, Set.empty)
    assert(newScores("a") == 0)
    assert(newScores("b") == 99)
  }

  it should "not apply score rules for players with no score" in {
    val players = Seq(PlayerCards("a", Seq(C("DK"), C("HK")), None), PlayerCards("b", Seq.empty, None))
    val scores  = Map("a" -> 80, "b" -> 70)
    val newScores =
      GameSeriesLogic.applyPointRules(
        Seq(twoKingsRule, seventyPointsRule),
        players,
        scores,
        excludedFromScoreRules = Set("a", "b")
      )
    assert(newScores("a") == 0)
    assert(newScores("b") == 70)
  }
}
