export interface CardType { id: string; gameRepresentation: Array<string>; endValue: number }
export interface DrawableCardType { card: CardType; drawable: boolean }

export interface PileType { top: Array<CardType>; drawable: Array<DrawableCardType>; bottom: number }

export interface PlayerInfoType {
  name: string;
  isMe: boolean;
  isCurrentPlayer: boolean;
  cards: Array<CardType> | number;
  score: number;
  scoreDiff?: Array<number>;
}