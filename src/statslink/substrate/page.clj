(ns statslink.substrate.page
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
             [statslink.substrate.remote :as remote]]
   :export [MODULE]})

(def.js PAGE_SPACE "statstrade/v1-slim")

(def.js PAGE_GROUP "currency")

(defn.js page-session
  "gets the serializable session owned by the substrate node"
  {:added "0.1"}
  [context]
  (return (or (substrate/get-service (. context ["node"]) "session")
              {})))

(defn.js currency-list
  "lists all currencies"
  {:added "0.1"}
  [context]
  (return
   (remote/view
    (-/page-session context)
    "Currency"
    {:select-method "all"
     :return-method "default"})))

(defn.js currency-brief
  "gets a brief currency record"
  {:added "0.1"}
  [context currency-id]
  (when (k/nil? currency-id)
    (return []))
  (return
   (remote/view
    (-/page-session context)
    "Currency"
    {:return-method "info"
     :return-id currency-id})))

(defn.js currency-detail
  "gets a detailed currency record"
  {:added "0.1"}
  [context currency-id]
  (when (k/nil? currency-id)
    (return []))
  (return
   (remote/view
    (-/page-session context)
    "Currency"
    {:return-method "default"
     :return-id currency-id})))

(defn.js currency-write
  "writes or deletes a currency through the worker-owned substrate session"
  {:added "0.1"}
  [context account-id operation payload]
  (when (or (k/nil? account-id)
            (k/nil? payload))
    (return []))
  (var session (-/page-session context))
  (var call
       (cond (== operation "delete")
             (remote/currency-delete session account-id payload)

             :else
             (remote/currency-set session account-id payload)))
  (return call))

(defn.js currency-create
  "creates a currency"
  {:added "0.1"}
  [context account-id payload]
  (return (-/currency-write context account-id "set" payload)))

(defn.js currency-modify
  "modifies or deletes a currency"
  {:added "0.1"}
  [context account-id operation payload]
  (return (-/currency-write context account-id operation payload)))

(def.js PAGE_CURRENCY
  {:id "currency"
   :space -/PAGE_SPACE
   :group -/PAGE_GROUP
   :contract ["list" "brief" "detail" "create" "modify"]
   :models {:list   {:handler -/currency-list
                     :defaults {:args []}}
            :brief  {:handler -/currency-brief
                     :defaults {:args []}}
            :detail {:handler -/currency-detail
                     :defaults {:args []}}
            :create {:handler -/currency-create
                     :defaults {:args []}}
            :modify {:handler -/currency-modify
                     :defaults {:args []}}}})

(def.js MODULE (!:module))
