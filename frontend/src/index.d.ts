interface CardType { id: string, gameRepresentation: Array<string>, endValue: number }
export interface DrawableCardType { card: CardType, drawable: boolean }
export interface PileType { top: Array<CardType>, drawable: Array<DrawableCardType>, bottom: number }
