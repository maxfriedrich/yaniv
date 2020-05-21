import { h } from 'preact';

import { CardType, PileType, LastActionType, LastActionThrownType, LastActionDrawnType, LastActionDrawThrownType } from './api';
import { Card } from './card';

const drawCard = (drawAction: (card: CardType) => void, card: CardType) => () =>
  drawAction(card);

export interface PileProps {
  pile?: PileType;
  disabled: boolean;
  drawAction: (card: CardType) => void;
  lastActionTmp?: LastActionType;
}

const isLastActionDrawThrow = (card: CardType, lastAction?: LastActionType): boolean => {
  console.log(lastAction, card);
  switch(lastAction?.type) {
    case "thrown": return ((lastAction as LastActionThrownType).cards as CardType[]).map(c => c.id).includes(card.id);
    case "drawn": return (lastAction as LastActionDrawnType).source === card.id;
    case "drawThrown": return (lastAction as LastActionDrawThrownType).card.id === card.id;
    default: return false;
  }
}

const isLastActionThrow = (lastAction?: LastActionType): boolean => {
  console.log(lastAction);
  switch (lastAction?.type) {
    case "thrown": return true;
    default: return false;
  }
}

export const Pile = ({ pile, disabled, drawAction, lastActionTmp }: PileProps) => {
  const animateThrowClass = isLastActionThrow(lastActionTmp) ? 'animate-throw' : '';
  return (
    <div class="pile-container">
      <div className={`top-container ${animateThrowClass}`}>
        {pile?.top.map(card => (
          <Card card={card} playable={false} classes={`pile-card ${isLastActionDrawThrow(card, lastActionTmp) ? 'animate-drawthrow' : ''}`} />
        ))}
      </div>
      <div className={`drawable-container ${animateThrowClass}`}>
        {pile?.drawable.map(pileCard => (
          <Card
            card={pileCard.card}
            playable={!disabled && pileCard.drawable}
            classes="pile-card"
            action={drawCard(drawAction, pileCard.card)}
          />
        ))}
      </div>
      {pile?.bottom || 0 > 0 ? (
        <Card playable={false} backside classes={`pile-card pile-bottom ${animateThrowClass}`} />
      ) : (
          <div />
        )}
    </div>
  )
}
