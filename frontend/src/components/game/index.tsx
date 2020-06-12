import { h, Component } from 'preact';

import {
  CardType,
  GameSeriesStateType,
  FullPlayerCardsViewType,
  PartialPlayerCardsViewType,
  ExtendedPlayerInfoType
} from './api';

import { applyDrag } from './drag';

import { Flash, FlashTransitionTimeout } from '../common/flash';
import { Scores } from './scores';
import { Deck } from './deck';
import { Pile } from './pile';
import { Hand } from './hand';
import { NextGameControls } from './next';
import { Actions } from './actions';

interface GameComponentPropsType {
  gameId?: string;
  playerId?: string;
  debug?: boolean;
  path: string;
}

interface GameComponentStateType {
  selected: CardType[];
  sortedCards: CardType[];
  cardOnDeck?: CardType;
  serverState: GameSeriesStateType;
  flash?: string;
}

const DeckCardTransitionTimeout = 3000;

export class Game extends Component<
  GameComponentPropsType,
  GameComponentStateType
> {
  deckCardTransition?: ReturnType<typeof setTimeout>;
  flashTransition?: ReturnType<typeof setTimeout>;
  source?: EventSource;

  constructor() {
    super();
    this.state = {
      selected: [], // sorted
      sortedCards: [],
      flash: undefined,
      cardOnDeck: undefined,
      serverState: {
        id: 0,
        version: 0,
        me: '',
        players: [],
        state: {
          state: 'waitingForSeriesStart'
        },
        currentGame: undefined,
        scores: new Map(),
        scoresDiff: new Map()
      }
    };
  }

  isCurrentGame = (): boolean =>
    this.state.serverState.state.state === 'gameIsRunning';

  isPastGame = (): boolean =>
    this.state.serverState.state.state === 'waitingForNextGame' ||
    this.state.serverState.state.state === 'gameOver';

  isCurrentPlayer = (): boolean =>
    this.isCurrentGame() &&
    this.state.serverState.currentGame?.currentPlayer ===
      this.state.serverState.me;

  myName = (): string =>
    this.state.serverState.players.find(p => p.id === this.state.serverState.me)
      ?.name || '';

  playerCards = (playerId: string): CardType[] | number | undefined => {
    if (!this.isCurrentGame() && !this.isPastGame()) {
      return undefined;
    }
    if (playerId === this.state.serverState.me) {
      const myCards = this.state.serverState.currentGame?.me.cards;
      return this.isPastGame() ? myCards : myCards?.length;
    }
    const otherPlayers = this.state.serverState.currentGame?.otherPlayers;
    if (otherPlayers?.length === 0) {
      return undefined;
    }

    // TODO: this is not nice but I don't know any better
    return 'cards' in otherPlayers![0]
      ? (otherPlayers as FullPlayerCardsViewType[]).find(p => p.id === playerId)
          ?.cards
      : (otherPlayers as PartialPlayerCardsViewType[]).find(
          p => p.id === playerId
        )?.numCards;
  };

  playerInfo = (): ExtendedPlayerInfoType[] => {
    if (!this.state.serverState.players) return [];
    return this.state.serverState.players.map(player => ({
      name: player.name,
      isMe: player.id === this.state.serverState.me,
      isCurrentPlayer:
        this.isCurrentGame() &&
        player.id === this.state.serverState.currentGame?.currentPlayer,
      cards: this.playerCards(player.id) || [],
      score: this.state.serverState.scores[player.id],
      scoreDiff: this.state.serverState.scoresDiff[player.id]
    }));
  };

  // `excluded` can be null, a card, or an array of cards
  updateSortedCards = (serverCards: CardType[], excluded?: CardType[]) => {
    const isExcluded = id => {
      if (!excluded) return false;
      return excluded.some(e => e.id === id);
    };

    const serverCardIds = serverCards.map(c => c.id);
    const handCards = this.state.sortedCards.filter(c =>
      serverCardIds.includes(c.id)
    );
    const handCardIds = handCards.map(c => c.id);
    const newCards = serverCards
      .filter(c => !handCardIds.includes(c.id))
      .filter(c => !isExcluded(c.id));
    const result = handCards.concat(
      newCards.sort((a, b) => a.endValue - b.endValue)
    );
    return result;
  };

  componentDidMount = () => {
    console.log('game component did mount');
    fetch(`/rest/game/${this.props.gameId}/player/${this.props.playerId}/state`)
      .then(response => response.json())
      .then(initialServerState => {
        console.log('got initial server state:', initialServerState);
        const initialSortedCards = initialServerState.currentGame
          ? initialServerState.currentGame.me.cards.sort(
              (a, b) => a.endValue - b.endValue
            )
          : [];
        this.setState({
          serverState: initialServerState,
          sortedCards: initialSortedCards
        });
      })
      .catch(err => console.log(err));

    this.source = new EventSource(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/state/stream`
    );
    this.source.onmessage = event => {
      const newServerState = JSON.parse(event.data.substring(5));
      console.log('Got new server state from stream:', newServerState);
      if (newServerState.version <= this.state.serverState.version) {
        console.log(
          'not accepting server state with version',
          newServerState.version
        );
        return;
      }
      if (newServerState.state.state === 'gameIsRunning') {
        let newSortedCards;
        const isNewGame =
          this.state.serverState.state.state === 'waitingForNextGame';
        if (isNewGame) {
          newSortedCards = newServerState.currentGame.me.cards.sort(
            (a, b) => a.endValue - b.endValue
          );
        } else {
          const excluded = this.state.selected.concat(
            newServerState.currentGame.me.drawThrowable || []
          );
          newSortedCards = this.updateSortedCards(
            newServerState.currentGame.me.cards,
            excluded
          );
        }
        this.setState({
          serverState: newServerState,
          sortedCards: newSortedCards,
          cardOnDeck: undefined
        });
      } else {
        console.log('game over');
        if (this.deckCardTransition) clearTimeout(this.deckCardTransition);
        this.deckCardTransition = undefined;
        const newSortedCards = this.updateSortedCards(
          newServerState.currentGame.me.cards,
          []
        );
        console.log(newSortedCards);
        this.setState({
          serverState: newServerState,
          selected: [],
          sortedCards: newSortedCards,
          cardOnDeck: undefined
        });
      }
    };
    this.source.onerror = error => console.log(error);
  };

  componentWillUnmount = () => {
    if (this.deckCardTransition) clearTimeout(this.deckCardTransition);
    this.source?.close();
  };

  flash = (text: string) => {
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

  selectCard = (card: CardType) => () => {
    const newSelection = this.state.selected.concat([card]);
    const newSortedCards = this.state.sortedCards.filter(c => c.id !== card.id);
    this.setState({ selected: newSelection, sortedCards: newSortedCards });
  };

  unselectCard = (card: CardType) => () => {
    const newSelection = this.state.selected.filter(c => c.id !== card.id);
    const newSortedCards = this.state.sortedCards.concat([card]);
    this.setState({ selected: newSelection, sortedCards: newSortedCards });
  };

  updateSelectedOnDrop = e =>
    this.setState({ selected: applyDrag(this.state.selected, e) });
  updateSortedCardsOnDrop = e =>
    this.setState({ sortedCards: applyDrag(this.state.sortedCards, e) });

  isCurrentPlayerThrow = (): boolean =>
    this.isCurrentPlayer() &&
    this.state.serverState.currentGame?.nextAction === 'throw' &&
    this.state.serverState.currentGame.ending == null;
  isCurrentPlayerDraw = (): boolean =>
    this.isCurrentPlayer() &&
    this.state.serverState.currentGame?.nextAction === 'draw' &&
    this.state.serverState.currentGame.ending == null;

  isThrowDisabled = () =>
    !this.isCurrentPlayerThrow() || this.state.selected.length === 0;
  isYanivDisabled = () =>
    !this.isCurrentPlayerThrow() ||
    this.state.selected.length > 0 ||
    this.state.sortedCards.map(c => c.endValue).reduce((acc, x) => acc + x, 0) >
      5;

  drawFromPile = (card: CardType) => {
    console.log('Drawing from pile: ', card.id);
    fetch(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/draw`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ source: card.id })
      }
    )
      .then(response => response.json())
      .then(newServerState => {
        if ('error' in newServerState) {
          this.flash(newServerState.error);
        } else if (newServerState.state.state === 'gameIsRunning') {
          const newSortedCards = this.updateSortedCards(
            newServerState.currentGame.me.cards,
            newServerState.currentGame.me.drawThrowable
          );
          this.setState({
            serverState: newServerState,
            sortedCards: newSortedCards
          });
        } else {
          this.setState({ serverState: newServerState });
        }
      })
      .catch(err => this.flash(err));
  };

  drawFromDeck = () => {
    console.log('Drawing from deck');
    fetch(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/draw`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ source: 'deck' })
      }
    )
      .then(response => response.json())
      .then(newServerState => {
        // debugger;
        if ('error' in newServerState) {
          this.flash(newServerState.error);
        } else {
          this.setState({
            serverState: newServerState,
            cardOnDeck: newServerState.currentGame.me.drawThrowable
          });
          if (this.deckCardTransition) clearTimeout(this.deckCardTransition);
          this.deckCardTransition = setTimeout(() => {
            if (this.state.cardOnDeck) {
              console.log('scheduled');
              const newSortedCards = this.updateSortedCards(
                newServerState.currentGame.me.cards,
                this.state.selected
              );
              this.setState({
                sortedCards: newSortedCards,
                cardOnDeck: undefined
              });
            }
          }, DeckCardTransitionTimeout);
        }
      })
      .catch(err => this.flash(err));
  };

  throw = () => {
    console.log(this.state);
    const cardsToThrow = this.state.selected.map(card => card.id);
    console.log('Throwing cards: ' + this.state.selected.map(card => card.id));
    fetch(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/throw`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cards: cardsToThrow })
      }
    )
      .then(response => response.json())
      .then(newServerState => {
        if ('error' in newServerState) {
          this.flash(newServerState.error);
        } else {
          this.setState({ serverState: newServerState, selected: [] });
        }
      })
      .catch(err => this.flash(err));
  };

  drawThrow = () => {
    console.log(
      'Draw-throwing card: ' +
        this.state.serverState.currentGame?.me.drawThrowable?.id
    );
    fetch(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/drawThrow`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          card: this.state.serverState.currentGame?.me.drawThrowable?.id
        })
      }
    )
      .then(response => response.json())
      .then(newServerState => {
        if ('error' in newServerState) {
          this.flash(newServerState.error);
        } else {
          this.setState({
            serverState: newServerState,
            cardOnDeck: undefined,
            selected: []
          });
        }
      })
      .catch(err => this.flash(err));
  };

  yaniv = () => {
    console.log('Calling yaniv');
    fetch(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/yaniv`,
      {
        method: 'POST'
      }
    )
      .then(response => response.json())
      .then(newServerState => {
        if ('error' in newServerState) {
          this.flash(newServerState.error);
        } else {
          this.setState({ serverState: newServerState, selected: [] });
        }
      })
      .catch(err => this.flash(err));
  };

  nextGame = () => {
    console.log('next game');
    fetch(
      `/rest/game/${this.props.gameId}/player/${this.props.playerId}/next`,
      {
        method: 'POST'
      }
    )
      .then(response => response.json())
      .then(newServerState => {
        if ('error' in newServerState) {
          this.flash(newServerState.error);
        }
      })
      .catch(err => this.flash(err));
  };

  render = (
    { debug }: GameComponentPropsType,
    {
      selected,
      sortedCards,
      cardOnDeck,
      serverState,
      flash
    }: GameComponentStateType
  ) => (
    <div class="game">
      <Flash text={flash} dismissAction={this.dismissFlash} />
      <Scores
        players={this.playerInfo()}
        showScoreDiff={
          serverState.state.state === 'waitingForNextGame' ||
          serverState.state.state === 'gameOver'
        }
      />

      <div class="card bg-light my-2">
        {this.isCurrentGame() || this.isPastGame() ? (
          <div class="table-container">
            <Pile
              pile={serverState.currentGame?.pile}
              disabled={!this.isCurrentPlayerDraw()}
              drawAction={this.drawFromPile}
              lastActionTmp={serverState.currentGame?.lastAction}
            />
            <Deck
              deck={serverState.currentGame?.deck}
              cardOnDeck={cardOnDeck}
              disabled={!this.isCurrentPlayerDraw()}
              drawAction={this.drawFromDeck}
              drawThrowAction={this.drawThrow}
            />
          </div>
        ) : (
          <div class="table-container" />
        )}
      </div>

      <div class="card my=2">
        <div class="hand-container">
          <div class="card-header">
            {serverState.currentGame?.ending ? (
              <NextGameControls
                players={serverState.players}
                currentGame={serverState.currentGame}
                seriesState={serverState.state}
                alreadyAccepted={
                  serverState.state.acceptedPlayers?.includes(serverState.me) ||
                  false
                }
                nextGameAction={this.nextGame}
              />
            ) : (
              <Actions
                throwDisabled={this.isThrowDisabled()}
                throwAction={this.throw}
                yanivDisabled={this.isYanivDisabled()}
                yanivAction={this.yaniv}
              />
            )}
          </div>

          <Hand
            id="selected-container"
            active={this.isCurrentPlayerThrow()}
            inactiveSortingAllowed={false}
            cards={selected}
            onDrop={this.updateSelectedOnDrop}
            cardAction={this.unselectCard}
          />
          <Hand
            active={this.isCurrentPlayerThrow()}
            classes="border-top"
            inactiveSortingAllowed={true}
            cards={sortedCards}
            onDrop={this.updateSortedCardsOnDrop}
            cardAction={this.selectCard}
          />
        </div>
      </div>
      {debug ? (
        <pre>state: {JSON.stringify(serverState, undefined, 2)}</pre>
      ) : (
        <div />
      )}
    </div>
  );
}
