import { h } from 'preact';

import { Container, Draggable } from 'react-smooth-dnd';

import { Card } from './card';
import { CardType } from './api';

export interface HandProps {
  id?: string;
  classes?: string;
  active: boolean;
  inactiveSortingAllowed?: boolean;
  cards: CardType[];
  onDrop: (event) => void;
  cardAction: (card: CardType) => () => void;
}

const getChildPayload = (cards: CardType[]) => (i: number): CardType => cards[i];

export const Hand = ({id, active, classes, inactiveSortingAllowed, cards, onDrop, cardAction}: HandProps) => (
	<div id={id} className={`draggable-container py-2 ${active ? 'active' : 'inactive'} ${classes}`}>
		{active || inactiveSortingAllowed ? (
			<Container groupName="player-cards" orientation="horizontal" getChildPayload={getChildPayload(cards)} onDrop={onDrop}>
				{
					cards.map(c => (
						<Draggable key={c.id}>
							<Card card={c} playable={active} action={cardAction(c)} />
						</Draggable>
					))
				}
			</Container>
		) : <div />}
	</div>
)
