(ns iberia.substrate-hook
  (:require [lang.core :as l]
            [std.lib :as h]))

(l/script :js
  {:runtime :websocket
   :config {:id :v1-slim/web
            :bench false
            :emit {:native {:suppress true}
                   :lang/jsx false}
            :notify {:type :webpage :path "dev/notify"}}
   :require [[js.react.ext-model :as ext-model]]
   :export [MODULE]})

(defn.js listenSuccess
  "subscribes to a substrate event-model success value"
  {:added "0.1"}
  [model args opts]
  (return
   (ext-model/listenSuccess
    model
    (or args [])
    (or opts {}))))

(defn.js refresh
  "sets model args and refreshes through ext-model"
  {:added "0.1"}
  [model args]
  (return
   (ext-model/refresh-args
    model
    (or args []))))

(def.js MODULE (!:module))
