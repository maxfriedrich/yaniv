const cardsStrings = (cards) => {
	if (!cards) {
		return [];
	}
	if (typeof cards === 'number') {
		return [cards];
	}
	return cards.sort((a, b) => a.endValue - b.endValue).map(c => c.gameRepresentation.join(''));
};

const renderScoreDiff = (scores) => {
	const rendered = [];
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

const listGroupClasses = (active) => `${basicClasses} ${active ? activeClass : ''}`;

const meBadgeClasses = (active) => `badge ${active ? 'badge-light' : 'badge-primary'}`;

export const Scores = ({ players, showScoreDiff }) => (
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
						{(showScoreDiff && (player.scoreDiff || player.scoreDiff === 0)) ? <div class="player-score-diff small text-muted">{renderScoreDiff(player.scoreDiff)}</div> : <div />}
					</div>
				</li>
			)
			)}
		</ul>
	</div>
);