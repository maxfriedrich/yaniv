import { h, render } from 'preact';
import Router from 'preact-router';

import 'bootstrap/scss/bootstrap';

import { Join } from './join';
import { Game } from './game';
import { EventSourceTest } from './es';

render(
	<div class="container">
		<nav class="navbar navbar-light bg-light">
			<a class="navbar-brand" href="/">Yaniv</a>
		</nav>
		<Router>
			<Join path="/" />
			<Join path="/join/:gameId" />
			<Join path="/join/:gameId/player/:playerId" />
			<Game path="/game/:gameId/player/:playerId" />
			<EventSourceTest path="/es" />
		</Router>
	</div>
	, document.body
);
