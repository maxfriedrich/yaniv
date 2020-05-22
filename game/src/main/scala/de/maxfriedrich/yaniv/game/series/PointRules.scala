package de.maxfriedrich.yaniv.game.series

import de.maxfriedrich.yaniv.game.{Card, Cards}

sealed trait PointRuleCondition
case class Score(score: Int)      extends PointRuleCondition
case class Hand(cards: Set[Card]) extends PointRuleCondition

case class PointRule(condition: PointRuleCondition, newPoints: Int)

object PointRules {
  val Rules = Seq(
    PointRule(Hand(Cards.Jokers.toSet), 0),
    PointRule(Score(50), 25),
    PointRule(Score(100), 50)
  )
}
