import { h, Fragment } from 'preact';

import { CardType, ExtendedPlayerInfoType } from './api';

const cardsStrings = (cards?: CardType[] | number): string[] => {
  if (!cards) {
    return [];
  }
  if (typeof cards === 'number') {
    return [cards.toString()];
  }
  return cards
    .sort((a, b) => a.endValue - b.endValue)
    .map(c => c.gameRepresentation.join(''));
};

const renderScoreDiff = (scores?: number[]) => {
  const rendered: string[] = [];
  scores?.map(score => {
    if (score > 0) {
      rendered.push(`+${score}`);
    } else if (score < 0) {
      rendered.push(`–${-score}`);
    }
  });
  return rendered.length > 0 ? rendered.join(' ') : '';
};

const playersStartingWithMe = (players: ExtendedPlayerInfoType[]): ExtendedPlayerInfoType[] => {
  const meIndex = players.findIndex(p => p.isMe);
  return players.slice(meIndex).concat(players.slice(0, meIndex));
}

const basicClasses =
  'list-group-item py-2 d-flex justify-content-between align-items-middle';
const activeClass = 'active';

const listGroupClasses = (active: boolean) =>
  `${basicClasses} ${active ? activeClass : ''}`;

const meBadgeClasses = (active: boolean) =>
  `badge ${active ? 'badge-light' : 'badge-primary'}`;

export interface ScoresProps {
  players: ExtendedPlayerInfoType[];
  showScoreDiff: boolean;
}

export const Scores = ({ players, showScoreDiff }: ScoresProps) => (
  <div class="card my-2">
    <ul class="list-group list-group-flush">
      {playersStartingWithMe(players).map(player => (
        <li className={listGroupClasses(player.isCurrentPlayer)}>
          <div class="d-flex flex-column justify-content-center">
            <div>
              {player.name}&nbsp;
              {player.isMe ? (
                <span>
                  <span className={meBadgeClasses(player.isCurrentPlayer)}>
                    ME
                  </span>
                  &nbsp;
                </span>
              ) : (
                <Fragment />
              )}
              {cardsStrings(player.cards).map(card => (
                <span>
                  <span class="badge badge-secondary">{card}</span>{' '}
                </span>
              ))}
            </div>
          </div>
          <div className={`player-score ml-1 d-flex justify-content-end align-items-center ${showScoreDiff ? 'font-weight-bold' : ''}`}>
            {showScoreDiff && !player.scoreDiff?.includes(0) ? (
              <span class="player-score-diff mr-2 small text-muted">
                {renderScoreDiff(player.scoreDiff)}
              </span>
            ) : (
              <Fragment />
            )}
            <span>{player.score}</span>
          </div>
        </li>
      ))}
    </ul>
  </div>
);
