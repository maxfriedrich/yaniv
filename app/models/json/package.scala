package models

import play.api.libs.json.{JsError, JsString, JsSuccess, Json, JsonConfiguration, OWrites, Reads, Writes}
import play.api.libs.json.JsonNaming.SnakeCase

package object json {
  implicit val jsonConfig = JsonConfiguration(SnakeCase)

  implicit val cardReads: Reads[Card] = Reads {
    case s: JsString =>
      val cardOpt = Card.fromString(s.value)
      if (cardOpt.isEmpty)
        JsError(s"Not a valid card id: $s")
      else
        JsSuccess(cardOpt.get)
    case e => JsError(s"Not a valid card id: $e")
  }

  case class JsonCard(id: String, gameString: String, endValue: Int) extends Card

  implicit val jsonCardWrites: OWrites[JsonCard] = Json.writes[JsonCard]

  implicit val cardWrites: Writes[Card]                 = { card => Json.toJsObject(JsonCard(card.id, card.gameString, card.endValue)) }
  implicit val drawableCardReads: Reads[DrawableCard]   = Json.reads[DrawableCard]
  implicit val drawableCardWrites: Writes[DrawableCard] = Json.writes[DrawableCard]

  case class ThrowCardsClientResponse(cards: Seq[Card])
  implicit val throwCardsClientResponseReads: Reads[ThrowCardsClientResponse] = Json.reads[ThrowCardsClientResponse]

  implicit val drawSourceReads: Reads[DrawSource] = Reads {
    case JsString("deck") => JsSuccess(DeckSource)
    case JsString(s) =>
      Card.fromString(s) match {
        case Some(c) => JsSuccess(PileSource(c))
        case None    => JsError(s"Not a valid card: $s")
      }
    case s => JsError(s"Not a valid draw source: $s")
  }

  case class DrawCardClientResponse(source: DrawSource)
  implicit val drawCardClientResponseReads: Reads[DrawCardClientResponse] = Json.reads[DrawCardClientResponse]

  implicit val gameActionReads: Reads[GameAction] = Reads {
    case JsString("throw") => JsSuccess(Throw)
    case JsString("draw")  => JsSuccess(Draw)
    case s                 => JsError(s"Not a valid game action: $s")
  }

  implicit val gameActionWrites: Writes[GameAction] = Writes {
    case Throw => JsString("throw")
    case Draw  => JsString("draw")
  }

  implicit val playerReads: Reads[Player]            = Json.reads[Player]
  implicit val playerWrites: OWrites[Player]         = Json.writes[Player]
  implicit val playerViewWrites: OWrites[PlayerView] = Json.writes[PlayerView]

  implicit val pileWrites: OWrites[Pile]         = Json.writes[Pile]
  implicit val pileReads: Reads[Pile]            = Json.reads[Pile]
  implicit val pileViewWrites: OWrites[PileView] = Json.writes[PileView]

  implicit val gameStateWrites: OWrites[GameState]         = Json.writes[GameState]
  implicit val gameStateReads: Reads[GameState]            = Json.reads[GameState]
  implicit val gameStateViewWrites: OWrites[GameStateView] = Json.writes[GameStateView]
}
