(ns tao.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tao.core :refer [deftao]])
  (:require [goog.events :as events]
            [clojure.string :refer [split join replace trim]]
            [cljs.core.async :refer [put! <! chan]]
            [tao.utils :refer [deep-merge-with map->params log]]
            [secretary.core :as secretary :include-macros true :refer [defroute]])
  (:import goog.History
           goog.history.EventType))

(enable-console-print!)

(def history (History.))

(let [navigation (chan)]
  (events/listen history EventType/NAVIGATE #(put! navigation %))
  (go
   (while true
     (let [token (.-token (<! navigation))]
       (secretary/dispatch! token)))))

(.setEnabled history true)
(secretary/set-config! :prefix "#")

(defn navigate!
  ([route] (navigate! route {}))
  ([route query]
     (let [token (. history (getToken))
           old-route (first (split token "?"))
           new-route (str "/" route)
           query-string (map->params query)
           with-params (if (empty? query-string)
                         new-route
                         (str new-route "?" query-string))]
       (if (= old-route new-route)
         (. history (replaceToken with-params))
         (. history (setToken with-params))))))

(def state-mappings (atom {}))

(defn add-state-mapping [route translators]
  (swap! state-mappings conj [route translators]))

(defn get-path [key translators]
  (conj (get-in translators [key :path]) key))

(defn matcher->route [matcher args]
  (let [parts (rest (split matcher "/")) ; drop leading space
        as-keys (map #(if (re-find #":" %)
                        (keyword (replace % #":" ""))
                        %) parts)
        subbed (map #(if (keyword? %)
                       (% args)
                       %) as-keys)
        route (join "/" subbed)
        params (set (filter keyword? as-keys))
        query-keys (filter #(not (% params)) (keys args))
        query (select-keys args [query-keys])]
    {:route route
     :query query}))

(defn translate-state [[matcher {:keys [params query constants]}] state]
  (let [translators (merge params query)
        korks (keys translators)
        values (map (fn [key] (let [translator (key translators)
                                   path (conj (:path translator) key)
                                   value (get-in state path)
                                   translated (when-let [tfn (:->route translator)]
                                                (tfn value))]
                               translated)) korks)]
    (when (every? identity values)
      (matcher->route matcher (zipmap korks values)))))

(defn state->route [state]
  (let [mappings (map #(translate-state % state) @state-mappings)
        route (first (filter identity mappings))]
    route))

(defn update-history
  [{:keys [path new-state tag] :as tx-data}]
  (condp = tag
    :silent nil ;ignore
    (let [{:keys [route query]} (state->route new-state)]
      (navigate! route query))))

(def nav-chan (chan))
