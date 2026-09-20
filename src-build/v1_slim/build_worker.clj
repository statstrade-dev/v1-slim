(ns v1-slim.build-worker
  (:require [clojure.java.io :as io]
            [lang.core :as l]
            [statslink.substrate.worker]))

(defn worker-script
  "emits the packaged browser worker"
  []
  (l/emit-script
   '(do (statslink.substrate.worker/start))
   {:lang :js
    :layout :full}))

(defn -main
  [& _]
  (let [path "web/static/substrate/worker.js"]
    (io/make-parents path)
    (spit path (worker-script))
    (println (str "wrote " path))))
