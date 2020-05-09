const color = (card) => {
	if (card.id[0] === 'H' || card.id[0] === 'D') return 'red';
	if (card.id[0] === 'C' || card.id[0] === 'S') return '';
	return 'green';
};

const renderString = (card) => <span>{card.gameRepresentation[0]}<wbr />{card.gameRepresentation[1]}</span>;

const wrappedAction = (playable, action) => playable ? action : null;

export const Card = ({ card, classes, playable, backside, action }) => {
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