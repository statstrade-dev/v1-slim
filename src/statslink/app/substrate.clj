(ns statslink.app.substrate
  (:require [lang.core :as l]
            [std.lib :as h]))

(l/script :js
  {:runtime :websocket
   :config {:id :v1-slim/web
            :bench false
            :emit {:native {:suppress true}
                   :lang/jsx false}
            :notify {:type :webpage :path "dev/notify"}}
   :require [[xt.lang.base-lib :as k]
             [xt.substrate :as substrate]
             [xt.substrate.page-core :as page-core]
             [xt.substrate.page-proxy :as page-proxy]
             [xt.substrate.transport-browser :as browser-transport]
             [statslink.substrate.page :as page]]
   :export [MODULE]})

(defn.js browser-remote
  "uses the current browser origin for the worker HTTP calls"
  {:added "0.1"}
  []
  (var secured (== window.location.protocol "https:"))
  (var port (or window.location.port
                (:? secured "443" "80")))
  (return {:secured secured
           :host window.location.hostname
           :port port
           :auth-host window.location.hostname
           :auth-port port}))

(defn.js worker-url-source
  "creates a Worker source from a packaged worker URL"
  {:added "0.1"}
  [url]
  (return
   {"create_fn"
    (fn [listener]
      (var worker (new Worker url))
      (. worker
         (addEventListener
          "message"
          (fn [event]
            (return (listener (. event ["data"]))))
          false))
      (return worker))}))

(defn.js normalize-session
  "fills the worker session with browser-origin defaults"
  {:added "0.1"}
  [session]
  (var out (k/obj-assign
            (k/obj-clone (or session {}))
            {:remote (-/browser-remote)}))
  (var input (or session {}))
  (when (k/get-key input "remote")
    (k/set-key out "remote" (k/get-key input "remote")))
  (return out))

(defn.js connect
  "connects a browser proxy node to the packaged substrate worker"
  {:added "0.1"}
  [session]
  (var session (-/normalize-session session))
  (var node (substrate/node-create {:id "statstrade-v1-slim-browser"}))
  (page-proxy/install node)
  (return
   (. (browser-transport/connect-worker
       node
       {"transport_id" "worker"
        "source" (-/worker-url-source
                  (or (k/get-key session "worker-url")
                      "static/substrate/worker.js"))})
      (then
       (fn [connection]
         (var transport-id (k/get-key connection "transport_id"))
         (return
          (. (substrate/request
              node
              page/PAGE_SPACE
              "@/session/set"
              [session]
              {"transport_id" transport-id})
             (then
              (fn [_]
                (return
                 (. (page-proxy/group-open-proxy
                     node
                     page/PAGE_SPACE
                     page/PAGE_GROUP
                     {"transport_id" transport-id})
                    (then
                     (fn [group]
                       (return {"node" node
                                "connection" connection
                                "group" group
                                "session" session}))))))))))))))

(defn.js model
  "gets a worker-backed proxy event-model for a page mode"
  {:added "0.1"}
  [connection model-id]
  (var node (or (k/get-key connection "node")
                connection))
  (var [_ model] (page-core/model-ensure
                  node
                  page/PAGE_SPACE
                  page/PAGE_GROUP
                  model-id))
  (return model))

(defn.js path
  "returns the stable substrate path for a page mode"
  {:added "0.1"}
  [model-id]
  (return [page/PAGE_GROUP model-id]))

(defn.js disconnect
  "disconnects a browser proxy from the worker"
  {:added "0.1"}
  [connection]
  (var conn (k/get-key connection "connection"))
  (when conn
    (return (browser-transport/disconnect conn)))
  (return nil))

(def.js MODULE (!:module))
