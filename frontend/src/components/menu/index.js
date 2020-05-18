import { h, Component } from 'preact';
import { route } from 'preact-router';

import { Join } from './join';
import { Waiting } from './waiting';

export class Menu extends Component {

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
		const that = this;
		this.source = new EventSource(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/state/stream`);
		this.source.onmessage = (event) => {
			const newServerState = JSON.parse(event.data.substring(5));
			console.log('Got new server state from in-game stream:');
			console.log(newServerState);
			this.setState({ players: newServerState.players });
			if (newServerState.state && newServerState.state.state === 'gameIsRunning') {
				route(`/game/${that.props.gameId}/player/${that.props.playerId}`);
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
					//alert(JSON.stringify(data.error));
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
								//alert(JSON.stringify(data.error));
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
					//alert(JSON.stringify(data.error));
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
					//alert(JSON.stringify(data.error));
				}
				console.log('got create game response:', data);
				route(`/game/${this.props.gameId}/player/${this.props.playerId}`);
			})
			.catch(err => console.log(err));
	}

	joinLink = () => `${window !== undefined ? window.location.origin : null}/join/${this.props.gameId}`

	updateName = (e) => this.setState({ name: e.target.value })

	removePlayer = (playerId) => () => {
		console.log('remove player', playerId);
		fetch(`/rest/game/${this.props.gameId}/remove/${playerId}`, {
			method: 'POST'
		})
			.then(response => response.json())
			.then((data) => {
				if ('error' in data) {
					//alert(JSON.stringify(data.error));
				}
				console.log('got ok response:', data);
			})
			.catch(err => console.log(err));
	}

	render = ({ gameId, playerId, debug }, { players, name }) => (
		<div>
			{gameId ? (
				playerId ?
					<Waiting
						joinLink={this.joinLink()}
						players={players}
						me={playerId}
						buttonTitle='Start Game'
						action={this.startGame}
						removePlayer={this.removePlayer}
					/>
					: 
					<Join 
						name={this.state.name}
						updateName={this.updateName}
						buttonTitle={`Join ${players.length} Player${players.length !== 1 ? 's' : ''}`}
						action={this.joinGame} 
					/>
			) : 
				<Join
					name={this.state.name}
					updateName={this.updateName}
					buttonTitle='Create Game'
					action={this.createGame}
				/>
			}
			{debug ? (<pre>{JSON.stringify(this.state)} {JSON.stringify(this.props)}</pre>) : <div />}
		</div>
	)
}
