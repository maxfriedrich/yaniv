package de.maxfriedrich.yaniv.game

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import de.maxfriedrich.yaniv.game.series._
import play.api.libs.json._

import scala.util.{Failure, Success, Try}

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

  case class JsonCard(id: String, gameRepresentation: Seq[String], endValue: Int) extends Card

  implicit val cardWrites: Writes[Card] = Writes { card =>
    Json.writes[JsonCard].writes(JsonCard(card.id, card.gameRepresentation, card.endValue))
  }
  implicit val drawableCardReads: Reads[DrawableCard]   = Json.reads[DrawableCard]
  implicit val drawableCardWrites: Writes[DrawableCard] = Json.writes[DrawableCard]

  case class ThrowCardsClientResponse(cards: Seq[Card])
  implicit val throwCardsClientResponseReads: Reads[ThrowCardsClientResponse] = Json.reads[ThrowCardsClientResponse]

  implicit val drawSourceReads: Reads[DrawSource] = Reads {
    case JsString(Strings.Deck) => JsSuccess(DeckSource)
    case JsString(s) =>
      Card.fromString(s) match {
        case Some(c) => JsSuccess(PileSource(c))
        case None    => JsError(s"Not a valid card: $s")
      }
    case s => JsError(s"Not a valid draw source: $s")
  }

  implicit val drawSourceWrites: Writes[DrawSource] = Writes {
    case DeckSource       => JsString(Strings.Deck)
    case PileSource(card) => JsString(card.id)
  }

  case class DrawCardClientResponse(source: DrawSource)
  implicit val drawCardClientResponseReads: Reads[DrawCardClientResponse] = Json.reads[DrawCardClientResponse]

  case class DrawThrowCardClientResponse(card: Card)
  implicit val drawThrowCardClientResponseReads: Reads[DrawThrowCardClientResponse] =
    Json.reads[DrawThrowCardClientResponse]

  implicit val gameActionReads: Reads[GameAction] = Reads {
    case JsString(Strings.Throw) => JsSuccess(Throw)
    case JsString(Strings.Draw)  => JsSuccess(Draw)
    case s                       => JsError(s"Not a valid game action: ${s.toString}")
  }

  implicit val gameActionWrites: Writes[GameAction] = Writes {
    case Throw => JsString(Strings.Throw)
    case Draw  => JsString(Strings.Draw)
  }

  case class JsonStarted(`type`: String)
  case class JsonDrawn(`type`: String, source: DrawSource)
  case class JsonThrown(`type`: String, cards: Seq[Card])
  case class JsonDrawThrown(`type`: String, card: Card)

  implicit val lastGameActionReads: Reads[LastGameAction] = Reads { json =>
    json \ "type" match {
      case JsDefined(JsString(Strings.Started))    => JsSuccess(Started)
      case JsDefined(JsString(Strings.Drawn))      => Json.reads[Drawn].reads(json)
      case JsDefined(JsString(Strings.Thrown))     => Json.reads[Thrown].reads(json)
      case JsDefined(JsString(Strings.DrawThrown)) => Json.reads[DrawThrown].reads(json)
      case JsDefined(s)                            => JsError(s"Not a valid last action: ${s.toString}")
      case _                                       => JsError("Missing `type` field in LastGameAction object")
    }
  }

  implicit val lastGameActionWrites: Writes[LastGameAction] = Writes {
    case Started          => Json.writes[JsonStarted].writes(JsonStarted(Strings.Started))
    case Drawn(source)    => Json.writes[JsonDrawn].writes(JsonDrawn(Strings.Drawn, source))
    case Thrown(cards)    => Json.writes[JsonThrown].writes(JsonThrown(Strings.Thrown, cards))
    case DrawThrown(card) => Json.writes[JsonDrawThrown].writes(JsonDrawThrown(Strings.DrawThrown, card))
  }

  implicit val gameEndingReads: Reads[GameEnding] = Reads { json =>
    json \ "type" match {
      case JsDefined(JsString(Strings.Yaniv)) => Json.reads[Yaniv].reads(json)
      case JsDefined(JsString(Strings.Asaf))  => Json.reads[Asaf].reads(json)
      case JsDefined(JsString(Strings.Empty)) => Json.reads[EmptyHand].reads(json)
      case JsDefined(s)                       => JsError(s"Not a valid game ending type: ${s.toString}")
      case _                                  => JsError("Missing `type` field in GameEnding object")
    }
  }

  case class JsonYaniv(`type`: String, winner: PlayerId, points: Int)
  case class JsonAsaf(`type`: String, caller: PlayerId, points: Int, winner: PlayerId, winnerPoints: Int)
  case class JsonEmptyHand(`type`: String, winner: PlayerId)

  implicit val gameEndingWrites: Writes[GameEnding] = Writes {
    case Yaniv(caller, points) => Json.writes[JsonYaniv].writes(JsonYaniv(Strings.Yaniv, caller, points))
    case Asaf(caller, points, winner, winnerPoints) =>
      Json.writes[JsonAsaf].writes(JsonAsaf(Strings.Asaf, caller, points, winner, winnerPoints))
    case EmptyHand(player) => Json.writes[JsonEmptyHand].writes(JsonEmptyHand(Strings.Empty, player))
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

  case class JsonGameIsRunning(state: String)
  case class JsonWaitingForSeriesStart(state: String)
  case class JsonWaitingForNextGame(state: String, acceptedPlayers: Set[String])
  case class JsonGameOver(state: String, winner: PlayerId, acceptedPlayers: Set[String])

  implicit val highLevelStateWrites: Writes[HighLevelState] = Writes {
    case GameIsRunning =>
      Json.writes[JsonGameIsRunning].writes(JsonGameIsRunning(Strings.GameIsRunning))
    case WaitingForSeriesStart =>
      Json.writes[JsonWaitingForSeriesStart].writes(JsonWaitingForSeriesStart(Strings.WaitingForSeriesStart))
    case WaitingForNextGame(accepted) =>
      Json.writes[JsonWaitingForNextGame].writes(JsonWaitingForNextGame(Strings.WaitingForNextGame, accepted))
    case GameOver(winner, accepted) =>
      Json.writes[JsonGameOver].writes(JsonGameOver(Strings.GameOver, winner, accepted))
  }

  implicit val highLevelStateReads: Reads[HighLevelState] = Reads { json =>
    json \ Strings.State match {
      case JsDefined(JsString(Strings.GameIsRunning))         => JsSuccess(GameIsRunning)
      case JsDefined(JsString(Strings.WaitingForSeriesStart)) => JsSuccess(WaitingForSeriesStart)
      case JsDefined(JsString(Strings.WaitingForNextGame)) =>
        Json.reads[JsonWaitingForNextGame].reads(json).map(w => WaitingForNextGame(w.acceptedPlayers))
      case JsDefined(JsString(Strings.GameOver)) =>
        Json.reads[JsonGameOver].reads(json).map(w => GameOver(w.winner, w.acceptedPlayers))
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

  private val isoDateTimeFormatter                   = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  implicit val dateTimeWrites: Writes[LocalDateTime] = Writes { date => JsString(isoDateTimeFormatter.format(date)) }
  implicit val dateTimeReads: Reads[LocalDateTime] = Reads {
    case JsString(s) =>
      Try(LocalDateTime.parse(s, isoDateTimeFormatter)) match {
        case Failure(_)        => JsError(s"Not a valid datetime: $s")
        case Success(dateTime) => JsSuccess(dateTime)
      }
    case json => JsError(s"not a valid date time: $json")
  }

  implicit val gameStateWrites: Writes[GameState]         = Json.writes[GameState]
  implicit val gameStateReads: Reads[GameState]           = Json.reads[GameState]
  implicit val gameStateViewWrites: Writes[GameStateView] = Json.writes[GameStateView]

  implicit val gameSeriesWrites: Writes[GameSeriesState]                    = Json.writes[GameSeriesState]
  implicit val gameSeriesReads: Reads[GameSeriesState]                      = Json.reads[GameSeriesState]
  implicit val gameSeriesViewWrites: Writes[GameSeriesStateView]            = Json.writes[GameSeriesStateView]
  implicit val gameSeriesPreStartInfoWrites: Writes[GameSeriesPreStartInfo] = Json.writes[GameSeriesPreStartInfo]
}

object Strings {
  val Deck  = "deck"
  val Draw  = "draw"
  val Throw = "throw"

  val Started    = "started"
  val Drawn      = "drawn"
  val Thrown     = "thrown"
  val DrawThrown = "drawThrown"

  val Yaniv = "yaniv"
  val Asaf  = "asaf"
  val Empty = "empty"

  val State                 = "state"
  val WaitingForSeriesStart = "waitingForSeriesStart"
  val WaitingForNextGame    = "waitingForNextGame"
  val GameIsRunning         = "gameIsRunning"
  val GameOver              = "gameOver"
}
