import { h } from 'preact';

import { CardType } from '.'

const color = (card: CardType) => {
	if (card.id[0] === 'H' || card.id[0] === 'D') return 'red';
	if (card.id[0] === 'C' || card.id[0] === 'S') return '';
	return 'green';
};

const renderString = (card: CardType) => <span>{card.gameRepresentation[0]}<wbr />{card.gameRepresentation[1]}</span>;

const wrappedAction = (playable: boolean, action: () => void) => playable ? action : null;

export interface CardProps { 
	card?: CardType,
	classes?: string,
	playable: boolean,
	backside?: boolean, 
	action?: () => void
}

export const Card = ({ card, classes, playable, backside, action }: CardProps) => {
	const playableClass = playable ? 'playable': 'inactive';
	const backsideClass = backside ? 'backside' : '';
	return (
		<div className={`game-card ${playableClass} ${backsideClass} ${card ? color(card) : ''} ${classes}`}
			disabled={!playable}
			onClick={wrappedAction(playable, action)}
		>
			{card ? renderString(card) : ''}
		</div>
	);
};