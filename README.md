# yaniv [![CI](https://github.com/maxfriedrich/yaniv/workflows/CI/badge.svg)](https://github.com/maxfriedrich/yaniv/actions?query=workflow%3ACI) [![Production build](https://github.com/maxfriedrich/yaniv/workflows/Production%20build/badge.svg?branch=master)](https://github.com/maxfriedrich/yaniv/actions?query=workflow%3A%22Production+build%22)

A WIP implementation of the [Yaniv](https://en.wikipedia.org/wiki/Yaniv_(card_game)) card game
as a REST API with [Server-sent events](https://en.wikipedia.org/wiki/Server-sent_events) push
and a [Preact frontend](frontend).

Try it out on [maxfrie.uber.space](https://maxfrie.uber.space)! (automatically deployed from the master branch using the "production build" action)

## Development mode

- Run the backend on localhost:9000:
  
  ```bash
  sbt run
  ```

- Run the frontend on [localhost:8080](http://localhost:8080):
  
  ```bash
  cd frontend
  npm run dev
  ```

This will auto-reload on file changes.

## Production build

- Build the frontend into `public/frontend`:
  
  ```bash
  cd frontend
  npm run build
  ```

- Make a build of the Play app that will serve the frontend:

  ```bash
  sbt dist
  ```
