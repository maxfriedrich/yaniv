import { h, Component } from 'preact';
import { route } from 'preact-router';

export interface JoinComponentPropsType {
	playerId: string;
	gameId: string;
}

export interface JoinComponentStateType {
	players: [];
	name: string;
}

export class Join extends Component<JoinComponentPropsType, JoinComponentStateType> {
	source?: EventSource;

	constructor() {
		super();
		this.state = {
			players: [],
			name: ''
		};
	}

	getGameInfo = () => {
		console.log('trying to get game state...');
		if (!this.props.gameId) return;
		console.log('getting game state...');
		fetch(`/rest/game/${this.props.gameId}/preStartInfo`)
			.then(response => response.json())
			.then((initialServerState) => {
				console.log('got initial server state:', initialServerState);
				this.setState({ players: initialServerState.players });
			})
			.catch(err => console.log(err));
	}

	setupPreStartStream = () => {
		console.log('trying to get pre-game stream...');
		if (!this.props.gameId) return false;
		console.log('getting pre-game stream...');
		this.source = new EventSource(`/rest/game/${this.props.gameId}/preStartInfo/stream`);
		this.source.onmessage = (event) => {
			const newServerState = JSON.parse(event.data.substring(5));
			console.log('Got new server state from pre-stream:');
			console.log(newServerState);
			this.setState({ players: newServerState.players });
		};
		return true;
	}


	setupStream = () => {
		console.log('trying to get in-game stream...');
		if (!this.props.gameId || !this.props.playerId) return false;
		console.log('getting in-game stream...');
		this.source?.close()
		this.source = new EventSource(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/state/stream`);
		this.source.onmessage = (event) => {
			const newServerState = JSON.parse(event.data.substring(5));
			console.log('Got new server state from in-game stream:');
			console.log(newServerState);
			this.setState({ players: newServerState.players });
			if (newServerState.state && newServerState.state.state === 'gameIsRunning') {
				route(`/game/${this.props.gameId}/player/${this.props.playerId}`);
			}
		};
		return true;
	}

	componentDidMount = () => {
		console.log('component did mount');
		this.getGameInfo();
		if (!this.setupStream()) this.setupPreStartStream();
	}

	componentWillUnmount = () => {
		console.log('component will unmount');
		this.source?.close();
	}

	createGame = (e) => {
		e.preventDefault();
		console.log('Creating game...');
		fetch('/rest/game/new', {
			method: 'POST'
		})
			.then((response) => response.json())
			.then((data) => {
				console.log(data);
				if ('error' in data) {
					alert(JSON.stringify(data.error));
				}
				else {
					const gameId = data.id;
					this.props.gameId = gameId;
					console.log('Joining game...');
					fetch(`/rest/game/${this.props.gameId}/join`, {
						method: 'POST',
						headers: { 'Content-Type': 'application/json' },
						body: JSON.stringify({ name: this.state.name })
					})
						.then((response) => response.json())
						.then((data) => {
							console.log(data);
							if ('error' in data) {
								alert(JSON.stringify(data.error));
							}
							else {
								const playerId = data.id;
								console.log('Got player id:', playerId);
								this.props.playerId = playerId; // why is this necessary?
								this.getGameInfo();
								this.setupStream();
								route(`/join/${gameId}/player/${playerId}`);
							}
						})
						.catch(err => console.log(err));
				}
			})
			.catch((err) => console.log(err));
	}

	joinGame = (e) => {
		e.preventDefault();
		console.log('Joining game...');
		fetch(`/rest/game/${this.props.gameId}/join`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ name: this.state.name })
		})
			.then((response) => response.json())
			.then((data) => {
				console.log(data);
				if ('error' in data) {
					alert(JSON.stringify(data.error));
				}
				else {
					const playerId = data.id;
					console.log('Got player id:', playerId);
					this.props.playerId = playerId;
					this.getGameInfo();
					this.setupStream();
					route(`/join/${this.props.gameId}/player/${playerId}`);
				}
			})
			.catch(err => console.log(err));
	}

	startGame = (e) => {
		e.preventDefault();
		console.log('Starting game...');
		fetch(`/rest/game/${this.props.gameId}/start`, {
			method: 'POST'
		})
			.then(response => response.json())
			.then((data) => {
				if ('error' in data) {
					alert(JSON.stringify(data.error));
				}
				console.log('got create game response:', data);
				route(`/game/${this.props.gameId}/player/${this.props.playerId}`);
			})
			.catch(err => console.log(err));
	}

	joinLink = () =>  `${window.location.origin}/join/${this.props.gameId}`

	updateName = (e) => this.setState({ name: e.target.value })

	removePlayer = (playerId) => () => {
		console.log('remove player', playerId);
		fetch(`/rest/game/${this.props.gameId}/remove/${playerId}`, {
			method: 'POST'
		})
			.then(response => response.json())
			.then((data) => {
				if ('error' in data) {
					alert(JSON.stringify(data.error));
				}
				console.log('got ok response:', data);
			})
			.catch(err => console.log(err));
	}

	render = ({ gameId, playerId, debug }, { players, name }) => (
		<div>
			{gameId ? (
				playerId ? (
				// Joined, waiting for players
					<div class="card my-2">
						<div class="card-header">Waiting for players…</div>
						<div class="card-body">
							<p>Share this link: <a href={this.joinLink()}>{this.joinLink()}</a></p>
							<ul class="list-group">
								{players.map(player => (
									<li class="list-group-item py-2 d-flex justify-content-between align-items-middle">
										<span>{player.name}&nbsp;{player.id === playerId ? <span class="badge badge-primary">ME</span>: <span />}</span>
										{player.id === playerId ? <span /> : <button class="btn py-0 close" onClick={this.removePlayer(player.id)}>&times;</button>}
									</li>))}
							</ul>
							<button class="btn btn-primary my-2" disabled={players.length < 2}
								onClick={this.startGame}
							>Start Game</button>
						</div>
					</div>
				) : (
				// Join Game
					<div class="card my-2">
						<div class="card-body">
							<form>
								<div class="form-group">
									<input id="name" type="text" class="form-control" placeholder="Enter name" value={this.state.name}
										onChange={this.updateName}
									/>
								</div>
								<button class="btn btn-primary" disabled={!this.state.name || this.state.name.trim().length === 0} onClick={this.joinGame}>Join {players.length} Player{players.length !== 1 ? 's' : ''}</button>
							</form>
						</div>
					</div>
				)
			) : (
			// New Game
				<div class="card my-2">
					<div class="card-body">
						<form>
							<div class="form-group">
								<input type="text" class="form-control" id="name" placeholder="Enter name" value={this.state.name}
									onChange={this.updateName}
								/>
							</div>
							<button class="btn btn-primary" disabled={!this.state.name || this.state.name.trim().length === 0} onClick={this.createGame}>New Game</button>
						</form>
					</div>
				</div>
			)}
			{debug ? (<pre>{JSON.stringify(this.state)} {JSON.stringify(this.props)}</pre>) : <div />}
		</div>
	)
}
