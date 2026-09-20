(ns v1-slim.currency
  (:require [lang.core :as l]
            [std.lib :as h]))

(l/script :js
  {:runtime :websocket
   :config {:id :v1-slim/web
            :bench false
            :emit {:native {:suppress true}
                   :lang/jsx false}
            :notify {:type :webpage :path "dev/notify"}}
   :require [[js.react :as r :include [:fn]]
             [js.react.ext-model :as ext-model]
             [js.react-native :as n :include [:fn]]
             [xt.lang.base-lib :as k]
             [statslink.app.substrate :as substrate-app]
             [statslink.substrate.page :as page]
             [iberia.substrate-hook :as substrate-hook]]
   :export [MODULE]})

(defn.js useSubstrate
  "connects the browser-side proxy to the packaged worker"
  [session]
  (var [connection setConnection] (r/local))
  (r/init []
    (. (substrate-app/connect session)
       (then
        (fn [value]
          (setConnection value)))
       (catch
        (fn [error]
          (setConnection {:error error})))))
  (return connection))

(defn.js useCurrencyProps
  "binds the Currency page models to ext-model"
  [connection session currency-id]
  (var node (k/get-key connection "node"))
  (var list-model (substrate-app/model node "list"))
  (var detail-model (substrate-app/model node "detail"))
  (var create-model (substrate-app/model node "create"))
  (var modify-model (substrate-app/model node "modify"))
  (var entries
       (substrate-hook/listenSuccess
        list-model
        []
        {:default []}))
  (var detail
       (substrate-hook/listenSuccess
        detail-model
        [currency-id]
        {:default []}))
  (var account-id (k/get-key session "account-id"))
  (var write
       (fn [model args]
         (return
          (. (substrate-hook/refresh model args)
             (then
              (fn [result]
                (return
                 (. (substrate-hook/refresh list-model [])
                    (then
                     (fn []
                       (return result)))))))))))
  (return {:models {:list list-model
                    :detail detail-model
                    :create create-model
                    :modify modify-model}
           :entries entries
           :detail-entry detail
           :actions {:create (fn [payload]
                               (return
                                (write create-model
                                       [account-id payload])))
                     :modify (fn [operation payload]
                               (return
                                (write modify-model
                                       [account-id operation payload])))
                     :delete (fn [currency-id]
                               (return
                                (write modify-model
                                       [account-id "delete" currency-id])))}}))

(defn.js CurrencyReady
  "renders the Currency slice after its worker proxy is open"
  [props]
  (var connection (k/get-key props "connection"))
  (var session (or (k/get-key props "session") {}))
  (var currency-id (k/get-key props "currency-id"))
  (var iprops (-/useCurrencyProps connection session currency-id))
  (var entries (or (k/get-key iprops "entries") []))
  (var detail (or (k/get-key iprops "detail") []))
  (return
   [:% n/View
    {:style {:flex 1
             :padding 16}}
    [:% n/Text
     {:style {:fontSize 18}}
     "Currency substrate"]
    [:% n/Text
     (k/json-encode {:entries entries
                     :detail detail})]]))

(defn.js CurrencySlice
  "thin Currency list/detail/create/modify composition"
  {:added "0.1"}
  [props]
  (var props (or props {}))
  (var session (or (k/get-key props "session") {}))
  (var currency-id (k/get-key props "currency-id"))
  (var connection (-/useSubstrate session))
  (var error (and connection
                  (k/get-key connection "error")))
  (return
   (:? error
       [:% n/Text "Unable to connect to substrate worker"]
       connection
       (r/% -/CurrencyReady
            {:connection connection
             :session session
             :currency-id currency-id})
       [:% n/Text "Connecting to substrate worker..."])))

(def.js MODULE (!:module))
