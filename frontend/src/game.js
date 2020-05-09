import { h, Component } from 'preact';

import { Container, Draggable } from 'react-smooth-dnd';
import { applyDrag } from './drag';

import { Card } from './card';
import { Scores } from './scores';
import { Pile, Deck } from './table';
import { NextGameControls } from './next';
import './style';

export class Game extends Component {
	constructor() {
		super();
		this.state = {
			selected: [], // sorted
			sortedCards: [],
			cardOnDeck: null,
			serverState: {
				id: 0,
				version: 0,
				me: '',
				players: [],
				state: {
					state: 'waitingForSeriesStart'
				},
				currentGame: null,
				scores: {},
				scoresDiff: {}
			}
		};
	}

	isCurrentGame = () => this.state.serverState.state.state === 'gameIsRunning';

	isPastGame = () => this.state.serverState.state.state === 'waitingForNextGame' || this.state.serverState.state.state === 'gameOver';

	isCurrentPlayer = () => this.isCurrentGame() && this.state.serverState.currentGame.currentPlayer === this.state.serverState.me;

	myName = () => this.state.serverState.players.find(p => p.id === this.state.serverState.me).name;

	playerCards = (playerId) => {
		if (!this.isCurrentGame()) {
			return null;
		}
		if (playerId === this.state.serverState.me) {
			return this.state.serverState.currentGame.me.cards.length;
		}
		const otherPlayer = this.state.serverState.currentGame.otherPlayers.find(p => p.id === playerId);
		return otherPlayer ? otherPlayer.numCards : 0;
	}

	playerInfo = () => {
		if (!this.state.serverState.players) return [];
		return this.state.serverState.players.map(player => (
			{
				name: player.name,
				isMe: player.id === this.state.serverState.me,
				isCurrentPlayer: this.isCurrentGame() && player.id === this.state.serverState.currentGame.currentPlayer,
				cards: this.playerCards(player.id),
				score: this.state.serverState.scores[player.id],
				scoreDiff: this.state.serverState.scoresDiff[player.id]
			}
		));
	}

	updateSortedCards = (serverCards, exclude) => {
		// debugger;
		const isExcluded = (id) => (exclude && exclude.id === id);
		const serverCardIds = serverCards.map(c => c.id);
		const handCards = this.state.sortedCards.filter(c => serverCardIds.includes(c.id));
		const handCardIds = handCards.map(c => c.id);
		const newCards = serverCards.filter(c => !(handCardIds.includes(c.id))).filter(c => !isExcluded(c.id));
		const result = handCards.concat(newCards.sort((a, b) => a.endValue - b.endValue));
		console.log('New sorted cards:', result);
		return result;
	}

	componentDidMount = () => {
		console.log('game component did mount');
		fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/state`)
			.then(response => response.json())
			.then((initialServerState) => {
				console.log('got initial server state:', initialServerState);
				const initialSortedCards = (initialServerState.currentGame) ? initialServerState.currentGame.me.cards.sort((a, b) => a.endValue - b.endValue) : [];
				this.setState({ serverState: initialServerState, sortedCards: initialSortedCards });
			})
			.catch(err => console.log(err));

		this.source = new EventSource(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/state/stream`);
		this.source.onmessage = (event) => {
			console.log(event);
			// debugger;
			const newServerState = JSON.parse(event.data.substring(5));
			console.log('Got new server state from stream:', newServerState);
			if (newServerState.version <= this.state.serverState.version) {
				console.log('not accepting server state with version', newServerState.version);
				return;
			}
			if (newServerState.state.state === 'gameIsRunning') {
				const newSortedCards = this.updateSortedCards(newServerState.currentGame.me.cards, newServerState.currentGame.me.drawThrowable);
				this.setState({ serverState: newServerState, sortedCards: newSortedCards, cardOnDeck: null });
			}
			else {
				const newSortedCards = this.updateSortedCards(newServerState.currentGame.me.cards, []);
				this.setState({ serverState: newServerState, sortedCards: newSortedCards, cardOnDeck: null });
			}
		};
		this.source.onerror = (error) => console.log(error);
	}

	componentWillUnmount = () => {
		this.source = null;
	}

	selectCard = (card) => {
		const newSelection = this.state.selected.concat([card]);
		const newSortedCards = this.state.sortedCards.filter(c => c.id !== card.id);
		this.setState({ selected: newSelection, sortedCards: newSortedCards });
		console.log('Selecting card: ' + card.id);
	}

	unselectCard = (card) => {
		const newSelection = this.state.selected.filter(c => c.id !== card.id);
		const newSortedCards = this.state.sortedCards.concat([card]);
		this.setState({ selected: newSelection, sortedCards: newSortedCards });
		console.log('Unselecting card: ' + card.id);
	}

	drawFromPile = (card) => {
		console.log('Drawing from pile: ', card.id);
		fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/draw`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ source: card.id })
		})
			.then((response) => response.json())
			.then((newServerState) => {
				if ('error' in newServerState) {
					alert(JSON.stringify(newServerState.error));
				}
				else if (newServerState.state.state === 'gameIsRunning') {
					const newSortedCards = this.updateSortedCards(newServerState.currentGame.me.cards, newServerState.currentGame.me.drawThrowable);
					this.setState({ serverState: newServerState, sortedCards: newSortedCards });
				}
				else {
					this.setState({ serverState: newServerState });
				}
			}).catch(err => console.log(err));
	}

	drawFromDeck = () => {
		console.log('Drawing from deck');
		fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/draw`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ source: 'deck' })
		})
			.then((response) => response.json())
			.then((newServerState) => {
				// debugger;
				if ('error' in newServerState) {
					alert(JSON.stringify(newServerState.error));
				}
				else {
					this.setState({ serverState: newServerState, cardOnDeck: newServerState.currentGame.me.drawThrowable });
					setTimeout(() => {
						if (this.state.cardOnDeck) {
							console.log('scheduled');
							const newSortedCards = this.updateSortedCards(newServerState.currentGame.me.cards, []);
							this.setState({ sortedCards: newSortedCards, cardOnDeck: null });
						}
					}, 3000);
				}
			}).catch(err => console.log(err));
	}

	throw = () => {
		console.log(this.state);
		const cardsToThrow = this.state.selected.map(card => card.id);
		console.log('Throwing cards: ' + this.state.selected.map(card => card.id));
		fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/throw`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ cards: cardsToThrow })
		})
			.then((response) => response.json())
			.then((newServerState) => {
				if ('error' in newServerState) {
					alert(JSON.stringify(newServerState.error));
				}
				else {
					this.setState({ serverState: newServerState, selected: [] });
				}
			}).catch(err => console.log(err));
	}

	drawThrow = () => {
		console.log(this.state);
		console.log('Draw-throwing card: ' + this.state.serverState.currentGame.me.drawThrowable.id);
		fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/drawThrow`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ card: this.state.serverState.currentGame.me.drawThrowable.id })
		})
			.then((response) => response.json())
			.then((newServerState) => {
				if ('error' in newServerState) {
					alert(JSON.stringify(newServerState.error));
				}
				else {
					this.setState({ serverState: newServerState, cardOnDeck: null, selected: [] });
				}
			}).catch(err => console.log(err));
	}


	yaniv = () => {
		console.log(this.state);
		console.log('Calling yaniv');
		fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/yaniv`, {
			method: 'POST'
		})
			.then((response) => response.json())
			.then((newServerState) => {
				if ('error' in newServerState) {
					alert(JSON.stringify(newServerState.error));
				}
				else {
					this.setState({ serverState: newServerState, selected: [] });
				}
			}).catch(err => console.log(err));
	}

	nextGame = () => {
		console.log('next game');
		fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/next`, {
			method: 'POST'
		})
			.then((response) => response.json())
			.then((newServerState) => {
				if ('error' in newServerState) {
					alert(JSON.stringify(newServerState.error));
				}
				else {
					this.setState({ selected: [], sortedCards: [] });
				}
			}).catch(err => console.log(err));
	}

	render = ({ debug }, { selected, sortedCards, cardOnDeck, serverState }) => (
		<div class="game">
			<Scores players={this.playerInfo()} showScoreDiff={serverState && (serverState.state.state === 'waitingForNextGame' || serverState.state.state === 'gameOver')} />

			<div class="card bg-light my-2">
				{this.isCurrentGame() || this.isPastGame() ? (
					<div class="table-container">
						<Pile pile={serverState.currentGame.pile}
							disabled={!this.isCurrentGame() || !this.isCurrentPlayer() || serverState.currentGame.nextAction !== 'draw'}
							drawAction={this.drawFromPile}
						/>
						<Deck deck={serverState.currentGame.deck}
							cardOnDeck={cardOnDeck}
							disabled={!this.isCurrentGame() ||!this.isCurrentPlayer() || serverState.currentGame.nextAction !== 'draw'}
							drawAction={this.drawFromDeck}
							drawThrowAction={this.drawThrow}
						/>
					</div>
				) : (<div class="table-container" />)}
			</div>

			<div class="card my=2">
				<div class="hand-container">
					<div class="card-header">
						{((serverState.currentGame && serverState.currentGame.ending) || serverState.state.state === 'waitingForNextGame') ?
							<NextGameControls
								players={serverState.players}
								currentGame={serverState.currentGame}
								seriesState={serverState.state}
								alreadyAccepted={serverState.state.state === 'waitingForNextGame' && serverState.state.acceptedPlayers.includes(serverState.me)}
								nextGameAction={this.nextGame}
							/> : (
								<div>
									<button class="btn btn-primary mr-2" disabled={!this.isCurrentPlayer() || serverState.currentGame.nextAction !== 'throw' || serverState.currentGame.ending || selected.length === 0} onClick={this.throw}>Throw</button>
									<button class="btn btn-primary" disabled={!this.isCurrentPlayer() || serverState.currentGame.nextAction !== 'throw' || serverState.currentGame.ending || selected.length > 0} onClick={this.yaniv}>Yaniv</button>
								</div>)}
					</div>

					<div id="selected-container" className={`draggable-container py-2 ${this.isCurrentPlayer() && serverState.currentGame && serverState.currentGame.nextAction === 'throw' ? 'active' : 'inactive'}`}>
						{this.isCurrentPlayer() && serverState.currentGame && serverState.currentGame.nextAction === 'throw' ? (
							<Container groupName="player-cards" orientation="horizontal" getChildPayload={i => selected[i]} onDrop={e => this.setState({ selected: applyDrag(selected, e) })}>
								{
									selected.map(c => (
										<Draggable key={c.id}>
											<Card
												card={c}
												playable={this.isCurrentPlayer() && serverState.currentGame.nextAction === 'throw'}
												action={e => this.unselectCard(c)}
											/>
										</Draggable>
									))
								}
							</Container>
						) : <div />}
					</div>
					<div id="hand-container" class="draggable-container py-2 border-top">
						<Container groupName="player-cards" orientation="horizontal" getChildPayload={i => sortedCards[i]} onDrop={e => this.setState({ sortedCards: applyDrag(sortedCards, e) })}>
							{
								sortedCards.map(c => (<Draggable key={c.id}>
									<Card
										card={c}
										playable={this.isCurrentPlayer() && serverState.currentGame.nextAction === 'throw'}
										action={e => this.selectCard(c)}
									/>
								</Draggable>
								))
							}
						</Container>
					</div>
				</div>
			</div>
			{debug ? (<pre>
				state: {JSON.stringify(serverState, undefined, 2)}
			</pre>) : <div />}
		</div>
	)
}
