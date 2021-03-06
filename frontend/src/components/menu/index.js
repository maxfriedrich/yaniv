import { h, Component } from 'preact';
import { route } from 'preact-router';

import { Flash, FlashTransitionTimeout } from '../common/flash';
import { Join } from './join';
import { Waiting } from './waiting';

export class Menu extends Component {
  constructor() {
    super();
    this.state = {
      players: [],
      name: '',
      flash: undefined
    };
  }

  getGameInfo = () => {
    console.log('trying to get game state...');
    if (!this.props.gameId) return;
    console.log('getting game state...');
    fetch(`/rest/game/${this.props.gameId}/preStartInfo`)
      .then(response => response.json())
      .then(initialServerState => {
        console.log('got initial server state:', initialServerState);
        this.setState({ players: initialServerState.players });
      })
      .catch(err => this.flash(err));
  };

  setupPreStartStream = () => {
    console.log('trying to get pre-game stream...');
    if (!this.props.gameId) return false;
    console.log('getting pre-game stream...');
    this.source = new EventSource(
      `/rest/game/${this.props.gameId}/preStartInfo/stream`
    );
    this.source.onmessage = event => {
      const newServerState = JSON.parse(event.data.substring(5));
      console.log('Got new server state from pre-stream:');
      console.log(newServerState);
      this.setState({ players: newServerState.players });
    };
    return true;
  };

  setupStream = () => {
    console.log('trying to get in-game stream...');
    if (!this.props.gameId || !this.props.playerId) return false;
    console.log('getting in-game stream...');
    const that = this;
    this.source = new EventSource(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/state/stream`
    );
    this.source.onmessage = event => {
      const newServerState = JSON.parse(event.data.substring(5));
      console.log('Got new server state from in-game stream:');
      console.log(newServerState);
      this.setState({ players: newServerState.players });
      if (
        newServerState.state &&
        newServerState.state.state === 'gameIsRunning'
      ) {
        route(`/game/${that.props.gameId}/player/${that.props.playerId}`);
      }
    };
    return true;
  };

  componentDidMount = () => {
    this.getGameInfo();
    if (!this.setupStream()) this.setupPreStartStream();
  };

  componentWillUnmount = () => {
    this.source?.close();
  };

  flash = text => {
    if (this.flashTransition) clearTimeout(this.flashTransition);
    this.setState({ flash: text });
    this.flashTransition = setTimeout(
      () => this.setState({ flash: undefined }),
      FlashTransitionTimeout
    );
  };

  dismissFlash = () => {
    if (this.flashTransition) clearTimeout(this.flashTransition);
    this.setState({ flash: undefined });
  };

  createGame = e => {
    e.preventDefault();
    fetch('/rest/game/new', {
      method: 'POST'
    })
      .then(response => response.json())
      .then(data => {
        console.log(data);
        if ('error' in data) {
          this.flash(JSON.stringify(data.error));
        } else {
          const gameId = data.id;
          this.props.gameId = gameId;
          console.log('Joining game...');
          fetch(`/rest/game/${this.props.gameId}/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: this.state.name })
          })
            .then(response => response.json())
            .then(data => {
              console.log(data);
              if ('error' in data) {
                this.flash(JSON.stringify(data.error));
              } else {
                const playerId = data.id;
                console.log('Got player id:', playerId);
                this.props.playerId = playerId; // why is this necessary?
                this.getGameInfo();
                this.setupStream();
                route(`/join/${gameId}/player/${playerId}`);
              }
            })
            .catch(err => this.flash(err));
        }
      })
      .catch(err => this.flash(err));
  };

  joinGame = e => {
    e.preventDefault();
    console.log('Joining game...');
    fetch(`/rest/game/${this.props.gameId}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: this.state.name })
    })
      .then(response => response.json())
      .then(data => {
        console.log(data);
        if ('error' in data) {
          this.flash(data.error);
        } else {
          const playerId = data.id;
          console.log('Got player id:', playerId);
          this.props.playerId = playerId;
          this.getGameInfo();
          this.setupStream();
          route(`/join/${this.props.gameId}/player/${playerId}`);
        }
      })
      .catch(err => this.flash(err));
  };

  startGame = e => {
    e.preventDefault();
    console.log('Starting game...');
    fetch(`/rest/game/${this.props.gameId}/start`, {
      method: 'POST'
    })
      .then(response => response.json())
      .then(data => {
        if ('error' in data) {
          this.flash(data.error);
        }
        console.log('got create game response:', data);
        route(`/game/${this.props.gameId}/player/${this.props.playerId}`);
      })
      .catch(err => this.flash(err));
  };

  joinLink = () =>
    `${window !== undefined ? window.location.origin : null}/join/${
      this.props.gameId
    }`;

  updateName = e => this.setState({ name: e.target.value });

  removePlayer = playerId => () => {
    console.log('remove player', playerId);
    fetch(`/rest/game/${this.props.gameId}/remove/${playerId}`, {
      method: 'POST'
    })
      .then(response => response.json())
      .then(data => {
        if ('error' in data) {
          this.flash(data.error);
        }
      })
      .catch(err => this.flash(err));
  };

  addAI = () => {
    console.log('add AI');
    fetch(`/rest/game/${this.props.gameId}/addAI`, {
      method: 'POST'
    })
      .then(response => response.json())
      .then(data => {
        if ('error' in data) {
          this.flash(data.error);
        }
      })
      .catch(err => this.flash(err));
  };

  render = ({ gameId, playerId, debug }, { players, name, flash }) => (
    <div>
      <Flash text={flash} dismissAction={this.dismissFlash} />
      {gameId ? (
        playerId ? (
          <Waiting
            joinLink={this.joinLink()}
            players={players}
            me={playerId}
            buttonTitle="Start Game"
            action={this.startGame}
            removePlayer={this.removePlayer}
            addAI={this.addAI}
            onShareError={this.flash}
          />
        ) : (
          <Join
            name={this.state.name}
            updateName={this.updateName}
            buttonTitle={`Join ${players.length} Player${
              players.length !== 1 ? 's' : ''
            }`}
            action={this.joinGame}
          />
        )
      ) : (
        <Join
          name={this.state.name}
          updateName={this.updateName}
          buttonTitle="Create Game"
          action={this.createGame}
        />
      )}
      {debug ? (
        <pre>
          {JSON.stringify(this.state)} {JSON.stringify(this.props)}
        </pre>
      ) : (
        <div />
      )}
    </div>
  );
}
