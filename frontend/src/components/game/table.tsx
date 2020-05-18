import { h } from 'preact';

import { CardType, PileType } from './api';
import { Card } from './card';

const drawCard = (drawAction: (card: CardType) => void, card: CardType) => () =>
  drawAction(card);

export interface PileProps {
  pile: PileType;
  disabled: boolean;
  drawAction: (card: CardType) => void;
}

export const Pile = ({ pile, disabled, drawAction }: PileProps) => (
  <div class="pile-container">
    <div class="top-container">
      {pile.top.map(card => (
        <Card card={card} playable={false} classes="pile-card" />
      ))}
    </div>
    <div class="drawable-container">
      {pile.drawable.map(pileCard => (
        <Card
          card={pileCard.card}
          playable={!disabled && pileCard.drawable}
          classes="pile-card"
          action={drawCard(drawAction, pileCard.card)}
        />
      ))}
    </div>
    {pile.bottom > 0 ? (
      <Card playable={false} backside classes="pile-card pile-bottom" />
    ) : (
      <div />
    )}
  </div>
);

export interface DeckProps {
  deck?: number;
  disabled: boolean;
  cardOnDeck?: CardType;
  drawAction: () => void;
  drawThrowAction: () => void;
}

export const Deck = ({
  deck,
  cardOnDeck,
  disabled,
  drawAction,
  drawThrowAction
}: DeckProps) => (
  <div class="deck-container">
    {cardOnDeck ? (
      <Card
        classes={'deck-card'}
        card={cardOnDeck}
        playable
        action={drawThrowAction}
      />
    ) : (
      <Card
        classes={'deck-card'}
        backside
        playable={!disabled}
        action={drawAction}
      />
    )}
  </div>
);
