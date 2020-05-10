import { Card } from './card';

const drawCard = (drawAction, card) => () => drawAction(card);

export const Pile = ({ pile, disabled, drawAction }) => (
	<div class="pile-container">
		<div class="top-container">
			{pile.top.map(card => <Card card={card} playable={false} classes="pile-card" />)}
		</div>
		<div class="drawable-container">
			{pile.drawable.map(pileCard =>
				<Card card={pileCard.card} playable={!disabled && pileCard.drawable} classes="pile-card" action={drawCard(drawAction, pileCard.card)} />)}
		</div>
		{pile.bottom > 0 ? <Card playable={false} backside classes="pile-card pile-bottom" /> : <div />}
	</div>
);

export const Deck = ({ deck, cardOnDeck, disabled, drawAction, drawThrowAction }) => (
	<div class="deck-container">
		{cardOnDeck ? <Card classes={'deck-card'} card={cardOnDeck} playable action={drawThrowAction} /> :
			<Card classes={'deck-card'} backside playable={!disabled} action={drawAction} />}
	</div>
);
