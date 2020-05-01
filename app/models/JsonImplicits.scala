package models

import models.series._
import play.api.libs.json._

object JsonImplicits {
  case class JoinGameClientResponse(name: String)

  implicit val joinGameClientResponseReads: Reads[JoinGameClientResponse] = Json.reads[JoinGameClientResponse]

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

  implicit val cardWrites: Writes[Card] = Writes { card =>
    Json.writes[JsonCard].writes(JsonCard(card.id, card.gameString, card.endValue))
  }
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

  case class DrawThrowCardClientResponse(card: Card)
  implicit val drawThrowCardClientResponseReads: Reads[DrawThrowCardClientResponse] =
    Json.reads[DrawThrowCardClientResponse]

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
    json \ "type" match {
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

  implicit val playerInfoReads: Reads[PlayerInfo]   = Json.reads[PlayerInfo]
  implicit val playerInfoWrites: Writes[PlayerInfo] = Json.writes[PlayerInfo]

  implicit val playerReads: Reads[PlayerCards]        = Json.reads[PlayerCards]
  implicit val playerCardsWrites: Writes[PlayerCards] = Json.writes[PlayerCards]
  implicit val playerCardsViewWrites: Writes[PlayerCardsView] = Writes {
    case p: PartialPlayerCardsView => Json.writes[PartialPlayerCardsView].writes(p)
    case f: FullPlayerCardsView    => Json.writes[FullPlayerCardsView].writes(f)
  }

  implicit val pileWrites: Writes[Pile]         = Json.writes[Pile]
  implicit val pileReads: Reads[Pile]           = Json.reads[Pile]
  implicit val pileViewWrites: Writes[PileView] = Json.writes[PileView]

  case class JsonWaitingForSeriesStart(noCurrentGame: String)
  case class JsonWaitingForNextGame(noCurrentGame: String, acceptedPlayers: Set[String])
  case class JsonGameOver(noCurrentGame: String, winner: PlayerId)

  implicit val noCurrentGameWrites: Writes[NoCurrentGame] = Writes {
    case WaitingForSeriesStart =>
      Json.writes[JsonWaitingForSeriesStart].writes(JsonWaitingForSeriesStart("waitingForSeriesStart"))
    case WaitingForNextGame(accepted) =>
      Json.writes[JsonWaitingForNextGame].writes(JsonWaitingForNextGame("waitingForNextGame", accepted))
    case GameOver(winner) => Json.writes[JsonGameOver].writes(JsonGameOver("gameOver", winner))
  }

  implicit val noCurrentGameReads: Reads[NoCurrentGame] = Reads { json =>
    json \ "noCurrentGame" match {
      case JsDefined(JsString("waitingForSeriesStart")) => JsSuccess(WaitingForSeriesStart)
      case JsDefined(JsString("waitingForNextGame")) =>
        Json.reads[JsonWaitingForNextGame].reads(json).map(w => WaitingForNextGame(w.acceptedPlayers))
      case JsDefined(JsString("gameOver")) => Json.reads[JsonGameOver].reads(json).map(w => GameOver(w.winner))
      case _ => JsError("not a valid no current game")
    }
  }

  implicit val pointRuleConditionWrites: Writes[PointRuleCondition] = Writes {
    case h: Hand  => Json.writes[Hand].writes(h)
    case s: Score => Json.writes[Score].writes(s)
  }

  implicit val pointRuleConditionReads: Reads[PointRuleCondition] = Reads { json =>
    (json \ "score", json \ "cards") match {
      case (JsDefined(_), _) => Json.reads[Score].reads(json)
      case (_, JsDefined(_)) => Json.reads[Hand].reads(json)
      case _                 => JsError(s"Not a valid point rule condition: $json")
    }
  }

  implicit val pointRuleWrites: Writes[PointRule] = Json.writes[PointRule]
  implicit val pointRuleReads: Reads[PointRule]   = Json.reads[PointRule]

  implicit val gameConfigWrites: Writes[GameConfig]             = Json.writes[GameConfig]
  implicit val gameConfigReads: Reads[GameConfig]               = Json.reads[GameConfig]
  implicit val gameSeriesConfigWrites: Writes[GameSeriesConfig] = Json.writes[GameSeriesConfig]
  implicit val gameSeriesConfigReads: Reads[GameSeriesConfig]   = Json.reads[GameSeriesConfig]


  implicit val gameStateWrites: Writes[GameState]         = Json.writes[GameState]
  implicit val gameStateReads: Reads[GameState]           = Json.reads[GameState]
  implicit val gameStateViewWrites: Writes[GameStateView] = Json.writes[GameStateView]

  implicit val noCurrentGameOrGameStateViewWrites: Writes[Either[NoCurrentGame, GameStateView]] = Writes {
    case Left(ncg)  => Json.toJson(ncg)
    case Right(gsv) => Json.toJson(gsv)
  }

  implicit val noCurrentGameOrGameStateWrites: Writes[Either[NoCurrentGame, GameState]] = Writes {
    case Left(ncg)  => Json.toJson(ncg)
    case Right(gsv) => Json.toJson(gsv)
  }
  implicit val noCurrentGameOrGameStateViewReads: Reads[Either[NoCurrentGame, GameState]] = Reads { json =>
    json \ "noCurrentGame" match {
      case JsDefined(_) => json.validate[NoCurrentGame].map(Left(_))
      case _            => json.validate[GameState].map(Right(_))
    }
  }

  implicit val gameSeriesWrites: Writes[GameSeriesState]                    = Json.writes[GameSeriesState]
//  implicit val gameSeriesReads: Reads[GameSeriesState]                      = Json.reads[GameSeriesState]
  implicit val gameSeriesViewWrites: Writes[GameSeriesStateView]            = Json.writes[GameSeriesStateView]
  implicit val gameSeriesPreStartInfoWrites: Writes[GameSeriesPreStartInfo] = Json.writes[GameSeriesPreStartInfo]
}
