(ns statslink.substrate.remote
  (:require [lang.core :as l]
            [std.lib :as h]))

(l/script :xtalk
  {:require [[xt.lang.base-lib :as k]
             [xt.lang.util-http :as http]]
   :export [MODULE]})

(def.xt VIEW_ROUTE "api/view/q")

(def.xt CURRENCY_SET_ROUTE "api/super/currency-set")

(def.xt CURRENCY_DELETE_ROUTE "api/super/currency-delete")

(defn.xt remote
  "gets the serializable remote configuration"
  {:added "0.1"}
  [session]
  (return
   (or (k/get-key (or session {}) "remote")
       {:secured false
        :host "127.0.0.1"
        :port "8080"})))

(defn.xt url
  "builds a route URL from the serializable session"
  {:added "0.1"}
  [session path]
  (var remote (-/remote session))
  (var host (or (k/get-key remote "host") "127.0.0.1"))
  (var port (or (k/get-key remote "port") "8080"))
  (var secured (k/get-key remote "secured"))
  (return
   (k/cat "http"
          (:? secured "s" "")
          "://"
          host
          ":"
          port
          "/"
          path)))

(defn.xt call
  "calls a Statstrade HTTP route using the worker-owned session"
  {:added "0.1"}
  [session uri args]
  (var session (or session {}))
  (var headers {"Args" (k/json-encode args)})
  (var token (k/get-key session "token"))
  (when (k/is-string? token)
    (k/set-key headers "Token" token))
  (return
   (k/for:async [[res err] (http/fetch-call
                            (-/url session uri)
                            {:method "POST"
                             :headers headers
                             :body ""
                             :as "json"})]
     {:success (cond (== "error" (k/get-key res "status"))
                     (k/throw res)

                     :else
                     (return (k/get-key res "data")))
      :error (k/throw err)})))

(defn.xt view
  "runs a view query"
  {:added "0.1"}
  [session table query]
  (return (-/call session
                 -/VIEW_ROUTE
                 [table query])))

(defn.xt currency-set
  "sets or updates a currency"
  {:added "0.1"}
  [session account-id payload]
  (return (-/call session
                 -/CURRENCY_SET_ROUTE
                 [account-id payload])))

(defn.xt currency-delete
  "deletes a currency"
  {:added "0.1"}
  [session account-id currency-id]
  (return (-/call session
                 -/CURRENCY_DELETE_ROUTE
                 [account-id currency-id])))

(def.js MODULE (!:module))
