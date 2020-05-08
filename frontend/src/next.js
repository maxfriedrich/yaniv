const renderEnding = (players, ending) => {
	if (!ending) return '';
	if (ending.type === 'yaniv') {
		const caller = players.find(p => p.id === ending.winner);
		return `${caller.name} called Yaniv with ${ending.points} points!`;
	}
	if (ending.type === 'asaf') {
		const caller = players.find(p => p.id === ending.caller);
		const winner = players.find(p => p.id === ending.winner);
		return `${caller.name} called Yaniv with ${ending.points} points but ${winner.name} had ${ending.winnerPoints} points!`;
	}
	if (ending.type === 'empty') {
		const winner = players.find(p => p.id === ending.winner);
		return `${winner.name} has no more cards!`;
	}
};

export const NextGameControls = ({ players, currentGame, seriesState, nextGameAction, alreadyAccepted }) => (
	<div class="d-flex justify-content-between align-items-center">
		<span>{renderEnding(players, currentGame ? currentGame.ending.ending : null)}</span>
		{seriesState.state === 'gameOver' ? (<span>Game over!</span>) : (<span><button type="button" class="btn btn-primary" disabled={alreadyAccepted} onClick={nextGameAction}>
			{alreadyAccepted ? 'Waiting for other players…' : 'Next Game'}
		</button></span>)}
	</div>
);