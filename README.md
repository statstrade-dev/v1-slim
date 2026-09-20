# Statstrade v1-slim

A small, standalone Currency thin vertical for the Statstrade web surface.

The slice keeps application state in `xt.substrate` and puts the worker boundary between the browser UI and remote HTTP calls:

```
React / ext-model
        |
browser proxy node
        |
WebWorker: xt.substrate page group
        |
Currency HTTP adapter
```

## Slice boundary

- `src/statslink/substrate/page.clj` — Currency page contract and handlers.
- `src/statslink/substrate/remote.clj` — serializable session and HTTP route adapter.
- `src/statslink/substrate/worker.clj` — worker-owned node, page group, and session handler.
- `src/statslink/app/substrate.clj` — browser proxy and packaged-worker connection.
- `src/iberia/substrate_hook.clj` — `js.react.ext-model` subscription/refresh seam.
- `src/v1_slim/currency.clj` — small UI composition that consumes the proxy models.
- `src-build/v1_slim/build_worker.clj` — emits `web/static/substrate/worker.js`.

The slice deliberately keeps authentication outside the page model. The host passes a serializable session containing `token`, `account-id`, and optionally `worker-url` or `remote`.

## Build and test

```bash
lein test :only statslink.substrate.page-test
lein build-worker
```

The worker build writes `web/static/substrate/worker.js`. The browser-side default URL is `static/substrate/worker.js`.

## Host composition

A host page can mount the UI with a serializable session:

```clojure
[:% v1-slim.currency/CurrencySlice
 {:session {:account-id account-id
            :token token
            :worker-url "static/substrate/worker.js"}}]
```

The UI layer stays focused on `ext-model` inputs and outputs; the worker owns the substrate page models and remote calls.
