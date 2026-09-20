(ns statslink.substrate.page-test
  (:use code.test)
  (:require [lang.core :as l]
            [std.lib :as h]))

(l/script- :js
  {:runtime :basic
   :require [[xt.lang.base-lib :as k]
             [xt.substrate :as substrate]
             [xt.substrate.page-core :as page-core]
             [statslink.substrate.page :as page]
             [statslink.substrate.worker :as worker]]})

(fact:global
 {:setup [(l/rt:restart :js)]
  :teardown [(l/rt/stop)]})

^{:refer statslink.substrate.worker/create :added "0.1"}
(fact "creates the worker-owned Currency page group without a legacy client"
  (!.js
   (var node (worker/create {:token "test-token"
                             :remote {:secured false
                                      :host "127.0.0.1"
                                      :port "8080"}}))
   (var [_ list-model] (page-core/model-ensure
                        node
                        page/PAGE_SPACE
                        page/PAGE_GROUP
                        "list"))
   (var [_ modify-model] (page-core/model-ensure
                          node
                          page/PAGE_SPACE
                          page/PAGE_GROUP
                          "modify"))
   [(k/get-in page/PAGE_CURRENCY ["contract"])
    page/PAGE_SPACE
    page/PAGE_GROUP
    (. list-model ["::"])
    (. modify-model ["::"])
    (k/get-key (substrate/get-service node "session") "token")
    (k/get-key (substrate/get-handler node "@/session/set") "id")])
  => [["list" "brief" "detail" "create" "modify"]
      "statstrade/v1-slim"
      "currency"
      "event.model"
      "event.model"
      "test-token"
      "@/session/set"])
