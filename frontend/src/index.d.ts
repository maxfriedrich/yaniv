export interface CardType { id: string; gameRepresentation: Array<string>; endValue: number }
export interface DrawableCardType { card: CardType; drawable: boolean }

export interface PileType { top: Array<CardType>; drawable: Array<DrawableCardType>; bottom: number }

export interface PlayerInfoType { id: string; name: string }

export interface ExtendedPlayerInfoType {
  name: string;
  isMe: boolean;
  isCurrentPlayer: boolean;
  cards: Array<CardType> | number;
  score: number;
  scoreDiff?: Array<number>;
}

export interface GameStateType { ending?: GameResultType}
export interface SeriesStateType { 
  state: 'gameIsRunning' | 'waitingForNextGame' | 'waitingForSeriesStart' | 'gameOver';
  winner?: string; }
export interface GameResultType { ending: EndingType; points: Map<string, number> }
export interface EndingType { type: 'yaniv' | 'asaf' | 'empty'; winner; points; caller; winnerPoints }