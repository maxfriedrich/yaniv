import { h } from 'preact';

import { CardType, ExtendedPlayerInfoType } from '../..'

const cardsStrings = (cards?: CardType[] | number): string[] => {
	if (!cards) {
		return [];
	}
	if (typeof cards === 'number') {
		return [cards.toString()];
	}
	return cards.sort((a, b) => a.endValue - b.endValue).map(c => c.gameRepresentation.join(''));
};

const renderScoreDiff = (scores: number[]) => {
	const rendered: string[] = [];
	scores.map(score => {
		if (score > 0) {
			rendered.push(`+${score}`);
		}
		else if (score < 0) {
			rendered.push(`–${-score}`);
		}
	});
	return (rendered.length > 0) ? rendered.join(' ') : '';
};


const basicClasses = 'list-group-item py-2 d-flex justify-content-between align-items-middle';
const activeClass = 'active';

const listGroupClasses = (active: boolean) => `${basicClasses} ${active ? activeClass : ''}`;

const meBadgeClasses = (active: boolean) => `badge ${active ? 'badge-light' : 'badge-primary'}`;

export interface ScoresProps { players: ExtendedPlayerInfoType[]; showScoreDiff: boolean}

export const Scores = ({ players, showScoreDiff }: ScoresProps) => (
	<div class="card my-2">
		<ul class="list-group">
			{players.map(player => (
				<li className={listGroupClasses(player.isCurrentPlayer)}>
					<div class="d-flex flex-column justify-content-center"><div>{player.name}&nbsp;{player.isMe ? <span><span className={meBadgeClasses(player.isCurrentPlayer)}>ME</span>&nbsp;</span> : <span />}
						{cardsStrings(player.cards).map(card =>
							<span><span class="badge badge-secondary">{card}</span> </span>
						)}
					</div></div>
					<div class="player-score d-flex flex-column justify-content-center">
						<div>{player.score}</div>
						{(showScoreDiff && (player.scoreDiff || player.scoreDiff.includes(0))) ? <div class="player-score-diff small text-muted">{renderScoreDiff(player.scoreDiff)}</div> : <div />}
					</div>
				</li>
			)
			)}
		</ul>
	</div>
);