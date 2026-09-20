(ns statslink.substrate.worker
  (:require [lang.core :as l]
            [std.lib :as h]))

(l/script :js
  {:runtime :websocket
   :config {:id :v1-slim/worker
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

(defn.js configure-session
  "stores the serializable browser session on the worker node"
  {:added "0.1"}
  [node session]
  (var session (or session {}))
  (substrate/set-service node "session" session)
  (return session))

(defn.js init-page
  "runs the first page refresh after the browser session is installed"
  {:added "0.1"}
  [node]
  (var group (page-core/group-ensure
              node
              page/PAGE_SPACE
              page/PAGE_GROUP))
  (var current (k/get-key group "init"))
  (if current
    (return current)
    (do
      (var init (page-core/group-refresh
                 node
                 page/PAGE_SPACE
                 page/PAGE_GROUP
                 {}
                 nil))
      (k/set-key group "init" init)
      (return init))))

(defn.js session-set
  "installs the browser session and initializes the page group"
  {:added "0.1"}
  [space args request node]
  (-/configure-session node (k/first args))
  (return (-/init-page node)))

(defn.js create
  "creates the worker-owned substrate node and page group"
  {:added "0.1"}
  [session]
  (var node (substrate/node-create {:id "statstrade-v1-slim-worker"}))
  (-/configure-session node session)
  (page-proxy/install node)
  (substrate/register-handler node "@/session/set" -/session-set nil)
  (page-core/group-add-attach
   node
   page/PAGE_SPACE
   page/PAGE_GROUP
   (. page/PAGE_CURRENCY ["models"]))
  (return node))

(defn.js start
  "starts the packaged WebWorker endpoint"
  {:added "0.1"}
  []
  (var node (-/create {}))
  (return
   (browser-transport/boot-self
    node
    {"transport_id" "host"
     "target" self
     "ready" {"signal" "ready"
              "transport" "browser"
              "worker" "statstrade-v1-slim"}})))

(def.js MODULE (!:module))
