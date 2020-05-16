import { h } from 'preact'
import { PlayerInfoType, GameStateType, SeriesStateType, EndingType } from '../..';

const renderSeriesState = (players: PlayerInfoType[], state: SeriesStateType): string => {
	if (state.state === 'gameOver') {
		const winnerName = players.find(p => p.id === state.winner).name;
		return `Game over! Winner: ${winnerName}`;
	}
	return '';
};

const renderEnding = (players: PlayerInfoType[], ending?: EndingType): string => {
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

const renderNextAction = (state: string, alreadyAccepted: boolean) => {
	if (alreadyAccepted) {
		return 'Waiting for other players…';
	}
	if (state === 'gameOver') {
		return 'Start a new game?';
	}
	return 'Next Game';
};

export interface NextGameControlsProps { players: PlayerInfoType[]; currentGame?: GameStateType; seriesState: SeriesStateType; nextGameAction: () => void; alreadyAccepted: boolean }

export const NextGameControls = ({ players, currentGame, seriesState, nextGameAction, alreadyAccepted }: NextGameControlsProps) => (
	<div class="d-flex justify-content-between align-items-center">
		<div class="d-flex flex-column align-items-left mr-1">
			<div class="font-weight-bold">{renderSeriesState(players, seriesState)}</div>
			<div>{renderEnding(players, currentGame ? currentGame.ending.ending : null)}</div>
		</div>
		<div class="w-25 ml-1">
			<div class="d-flex justify-content-end align-items-end">
				<button type="button" class="btn btn-primary" disabled={alreadyAccepted} onClick={nextGameAction}>
					{renderNextAction(seriesState.state, alreadyAccepted)}
				</button>
			</div>
		</div>
	</div>
);