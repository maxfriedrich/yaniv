import { h, render } from 'preact';
import Router from 'preact-router';
import { CookiesProvider } from 'react-cookie';

import 'bootstrap/scss/bootstrap';

import Join from './components/join';
import Game from './components/game';
import { EventSourceTest } from './es';

import './style';

render(
	<div class="container">
		<nav class="navbar navbar-light bg-light">
			<a class="navbar-brand" href="/">Yaniv</a>
		</nav>
		<CookiesProvider>
			<Router>
				<Join path="/" />
				<Join path="/join/:gameId" />
				<Join path="/join/:gameId/player/:playerId" />
				<Game path="/game/:gameId/player/:playerId" />
				<EventSourceTest path="/es" />
			</Router>
		</CookiesProvider>
	</div>
	, document.body
);
