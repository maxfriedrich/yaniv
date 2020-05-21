import { h } from 'preact';

import { CardType } from './api';
import { Card } from './card';

export interface DeckProps {
  deck?: number;
  disabled: boolean;
  cardOnDeck?: CardType;
  drawAction: () => void;
  drawThrowAction: () => void;
}

export const Deck = ({
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
