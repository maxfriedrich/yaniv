import { h } from 'preact';
import Router from 'preact-router';

import 'bootstrap/scss/bootstrap';

import { Menu } from './menu';
import { Game } from './game';
import { EventSourceTest } from './es';

const App = () => (
  <div class="container">
    <nav class="navbar navbar-light bg-light">
      <a class="navbar-brand" href="/">
        🃏Yaniv
      </a>
    </nav>
    <Router>
      <Menu path="/" />
      <Menu path="/join/:gameId" />
      <Menu path="/join/:gameId/player/:playerId" />
      <Game path="/game/:gameId/player/:playerId" />
      <EventSourceTest path="/es" />
    </Router>
  </div>
);

export default App;
