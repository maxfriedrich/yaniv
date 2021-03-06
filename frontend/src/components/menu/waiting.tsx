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
  addAI: () => void;
  onShareError: (error) => void;
}

const isWebShareAvailable = (): boolean =>
  (window as any).navigator.share != null;

const webShare = (link: string, onShareError: (error) => void) => () => {
  const shareData = {
    title: 'Yaniv',
    text: "Let's play Yaniv!",
    url: link
  };
  (window as any).navigator.share(shareData).catch(onShareError);
};

export const Waiting = ({
  joinLink,
  players,
  me,
  buttonTitle,
  action,
  removePlayer,
  addAI,
  onShareError
}: WaitingProps) => (
  <div class="card my-2">
    <div class="card-header">Waiting for players…</div>
    <div class="card-body">
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
        <li class="list-group-item d-flex justify-content-start align-items-middle">
          {isWebShareAvailable() ? (
            <button
              class="btn btn-outline-primary btn-sm mr-2"
              onClick={webShare(joinLink, onShareError)}
            >
              🔗 Invite Players
            </button>
          ) : (
            <a href={joinLink}>
              <button class="btn btn-outline-primary btn-sm mr-2">
                🔗 Long-press to select the Join Link
              </button>
            </a>
          )}
          <button class="btn btn-outline-secondary btn-sm mr-2" onClick={addAI}>
            🤖 Add AI
          </button>
        </li>
      </ul>
      <button
        class="btn btn-primary mt-2 float-right"
        disabled={players.length < 2}
        onClick={action}
      >
        {buttonTitle}
      </button>
    </div>
  </div>
);
