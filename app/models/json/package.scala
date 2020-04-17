package models

import models.Game.PlayerId
import play.api.libs.json.{JsDefined, JsError, JsString, JsSuccess, Json, OWrites, Reads, Writes}

package object json {
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
    case s                 => JsError(s"Not a valid game action: ${s.toString}")
  }

  implicit val gameActionWrites: Writes[GameAction] = Writes {
    case Throw => JsString("throw")
    case Draw  => JsString("draw")
  }

  implicit val gameEndingReads: Reads[GameEnding] = Reads { json =>
    (json \ "type") match {
      case JsDefined(JsString("yaniv")) => Json.reads[Yaniv].reads(json)
      case JsDefined(JsString("asaf"))  => Json.reads[Asaf].reads(json)
      case JsDefined(s)                 => JsError(s"Not a valid game ending type: ${s.toString}")
      case _                            => JsError(s"Missing `type` field in GameEnding object")
    }
  }

  case class JsonYaniv(`type`: String, caller: PlayerId)
  case class JsonAsaf(`type`: String, caller: PlayerId, winner: PlayerId)

  implicit val gameEndingWrites: Writes[GameEnding] = Writes {
    case Yaniv(caller)        => Json.writes[JsonYaniv].writes(JsonYaniv("yaniv", caller))
    case Asaf(caller, winner) => Json.writes[JsonAsaf].writes(JsonAsaf("asaf", caller, winner))
  }

  implicit val gameResultReads: Reads[GameResult]   = Json.reads[GameResult]
  implicit val gameResultWrites: Writes[GameResult] = Json.writes[GameResult]

  implicit val playerReads: Reads[Player]    = Json.reads[Player]
  implicit val playerWrites: OWrites[Player] = Json.writes[Player]
  implicit val playerViewWrites: Writes[PlayerView] = Writes {
    case p: PartialPlayerView => Json.writes[PartialPlayerView].writes(p)
    case f: FullPlayerView    => Json.writes[FullPlayerView].writes(f)
  }

  implicit val pileWrites: OWrites[Pile]         = Json.writes[Pile]
  implicit val pileReads: Reads[Pile]            = Json.reads[Pile]
  implicit val pileViewWrites: OWrites[PileView] = Json.writes[PileView]

  implicit val gameStateWrites: OWrites[GameState]         = Json.writes[GameState]
  implicit val gameStateReads: Reads[GameState]            = Json.reads[GameState]
  implicit val gameStateViewWrites: OWrites[GameStateView] = Json.writes[GameStateView]
}
