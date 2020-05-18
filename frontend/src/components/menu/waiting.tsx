import { h } from 'preact';

// TODO: type should be in a common directory
import { PlayerInfoType } from '../game/api';

export interface WaitingProps {
  joinLink: string;
  players: PlayerInfoType[];
  me: string;
  buttonTitle: string;
  action: () => void;
  removePlayer: (playerId: string) => () => void;
}

export const Waiting = ({
  joinLink,
  players,
  me,
  buttonTitle,
  action,
  removePlayer
}: WaitingProps) => (
  <div class="card my-2">
    <div class="card-header">Waiting for players…</div>
    <div class="card-body">
      <p>
        Share this link: <a href={joinLink}>{joinLink}</a>
      </p>
      <ul class="list-group">
        {players.map(player => (
          <li class="list-group-item py-2 d-flex justify-content-between align-items-middle">
            <span>
              {player.name}&nbsp;
              {player.id === me ? (
                <span class="badge badge-primary">ME</span>
              ) : (
                <span />
              )}
            </span>
            {player.id === me ? (
              <span />
            ) : (
              <button class="btn py-0 close" onClick={removePlayer(player.id)}>
                &times;
              </button>
            )}
          </li>
        ))}
      </ul>
      <button
        class="btn btn-primary my-2"
        disabled={players.length < 2}
        onClick={action}
      >
        {buttonTitle}
      </button>
    </div>
  </div>
);
