export interface CardType { id: string; gameRepresentation: string[]; endValue: number }
export interface DrawableCardType { card: CardType; drawable: boolean }

export interface PileType { top: CardType[]; drawable: DrawableCardType[]; bottom: number }

export interface PlayerInfoType { id: string; name: string }

export interface ExtendedPlayerInfoType {
  name: string;
  isMe: boolean;
  isCurrentPlayer: boolean;
  cards: CardType[] | number;
  score: number;
  scoreDiff?: number[];
}

export interface PlayerCardsType {
  id: string;
  cards: CardType[];
  drawThrowable?: CardType;
}

export interface PlayerCardsViewType {
  id: string;
  numCards: number;
}

export interface FullPlayerCardsViewType extends PlayerCardsViewType {
  cards: CardType[];
}

export type PartialPlayerCardsViewType = PlayerCardsViewType

export interface GameStateType {
  me: PlayerCardsType;
  otherPlayers: PartialPlayerCardsViewType[] | FullPlayerCardsViewType[];
  currentPlayer: string;
  nextAction: 'draw' | 'throw';
  pile: PileType;
  deck: number;
  ending?: GameResultType;
}

export interface GameSeriesStateType {
  id: string | number;
  version: number;
  me: string;
  players: PlayerInfoType[];
  state: SeriesStateType;
  currentGame?: GameStateType;
  scores: Map<string, number>;
  scoresDiff: Map<string, number[]>;
}

// TODO: this is actually multiple types
export interface SeriesStateType { 
  state: 'gameIsRunning' | 'waitingForNextGame' | 'waitingForSeriesStart' | 'gameOver';
  winner?: string;
  acceptedPlayers?: string[];
}


export interface GameResultType { ending: EndingType; points: Map<string, number> }

// TODO: this is actually multiple types
export interface EndingType { type: 'yaniv' | 'asaf' | 'empty'; winner; points; caller?; winnerPoints? }
