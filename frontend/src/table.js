export const Pile = ({ pile, disabled, drawAction, cardColor }) => (
	<div class="pile-container">
		<div class="top-container">
			{pile.top.map(card =>
				(<div className={`game-card pile-card inactive ${cardColor(card)}`}>{card.gameString}</div>)
			)}
		</div>
		<div class="drawable-container">
			{pile.drawable.map(pileCard => {
				const classes = `game-card pile-card ${(!disabled && pileCard.drawable) ? 'playable' : 'inactive'} ${cardColor(pileCard.card)}`;
				return (<div className={classes} onClick={e => { if (!disabled && pileCard.drawable) drawAction(pileCard.card); }}>
					{pileCard.card.gameString}
				</div>);
			})}
		</div>
		{pile.bottom > 0 ? <div class="game-card pile-card backside pile-bottom" /> : <div />}
	</div>
);

export const Backside = ({ disabled, action }) => (
	<div class={`game-card deck-card deck backside ${(!disabled) ? 'playable' : 'inactive'}`}
		disabled={disabled}
		onClick={action}
	/>
);

export const CardOnDeck = ({ card, action, cardColor }) => (
	<div class={`game-card deck-card deck playable ${cardColor(card)}`}
		onClick={action}
	>{card.gameString}</div>
);


export const Deck = ({ deck, cardOnDeck, disabled, drawAction, drawThrowAction, cardColor }) => (
	<div class="deck-container">
		{cardOnDeck ? <CardOnDeck card={cardOnDeck} action={drawThrowAction} cardColor={cardColor} /> : <Backside disabled={disabled} action={drawAction} />}
	</div>
);