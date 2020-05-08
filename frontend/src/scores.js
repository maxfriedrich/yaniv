const cardsStrings = (cards) => {
	if (!cards) {
		return [];
	}
	if (typeof cards === 'number') {
		return [cards];
	}
	return cards.map(c => c.gameString);
};

const renderScoreDiff = (scores) => {
	const rendered = []
	scores.map(score => {
		if (score > 0) {
			rendered.push(`+${score}`);
		} else if (score < 0) {
			rendered.push(`–${-score}`);
		}
	});
	return (rendered.length > 0) ? `(${rendered.join(" ")})` : ''
};


const basicClasses = 'list-group-item d-flex justify-content-between align-items-middle';
const meClass = 'list-group-item'; // TODO: find a better way to highlight "me"
const activeClass = 'active';

const listGroupClasses = (me, active) =>
	`${basicClasses} ${me ? meClass : ''} ${active ? activeClass : ''}`;

const meBadgeClasses = (active) => `badge ${active ? 'badge-light' : 'badge-primary'}`;

export const Scores = ({ players, showScoreDiff }) => (
	<div class="card my-2">
		<ul class="list-group">
			{players.map(player => (
				<li className={listGroupClasses(player.isMe, player.isCurrentPlayer)}>
					<span>{player.name}&nbsp;{player.isMe ? <span><span className={meBadgeClasses(player.isCurrentPlayer)}>ME</span>&nbsp;</span> : <span />}
						{cardsStrings(player.cards).map(card =>
							<span><span class="badge badge-secondary">{card}</span> </span>
						)}
					</span>
					<span>
						<span class="player-score">{player.score}</span>
						{(showScoreDiff && (player.scoreDiff || player.scoreDiff === 0)) ? <span class="player-score-diff small">{renderScoreDiff(player.scoreDiff)}</span> : <span />}
					</span>
				</li>
			)
			)}
		</ul>
	</div>
);