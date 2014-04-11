(ns tao.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tao.core :refer [deftao]])
  (:require [goog.events :as events]
            [clojure.string :refer [split join replace trim]]
            [cljs.core.async :refer [put! <! chan]]
            [tao.utils :refer [deep-merge-with map->params log]]
            [secretary.core :as secretary :include-macros true :refer [defroute]])
  (:import goog.History
           goog.history.Html5History
           goog.history.EventType))

(enable-console-print!)

(def history (atom {}))

(defn init-history
  ([] (init-history {}))
  ([{:keys [push-state]
     :or {push-state true}
     :as opts}]
     (let [hist (if push-state
                  (let [h (Html5History.)]
                    (.setUseFragment h false)
                    (.setPathPrefix h "")
                    (.setEnabled h true)
                    h)
                  (let [h (History.)]
                    (.setEnabled h true)
                    (secretary/set-config! :prefix "#")
                    h))
           navigation (chan)]

       (events/listen hist EventType/NAVIGATE #(put! navigation %))
       (go
        (while true
          (let [nav (<! navigation)]
            (when (.-isNavigation nav)
              (secretary/dispatch! (.-token nav))))))

       (reset! history hist)
       (secretary/dispatch! (.getToken hist)))))

(defn navigate!
  ([route] (navigate! route {}))
  ([route query]
     (let [token (. @history (getToken))
           old-route (first (split token "?"))
           new-route (str "/" route)
           query-string (map->params query)
           with-params (if (empty? query-string)
                         new-route
                         (str new-route "?" query-string))]
       (if (= old-route new-route)
         (. @history (replaceToken with-params))
         (. @history (setToken with-params))))))

(def state-mappings (atom []))

(defn add-state-mapping [route translators]
  (swap! state-mappings conj [route translators]))

(defn get-path [key translators]
  (conj (get-in translators [key :path]) key))

(defn matcher->route [matcher route-params query-params]
  (let [parts (rest (split matcher "/")) ; drop leading space
        as-keys (map #(if (re-find #":" %)
                        (keyword (replace % #":" ""))
                        %) parts)
        subbed (map #(if (keyword? %)
                       (% route-params)
                       %) as-keys)
        route (join "/" subbed)]
    route))

(defn translate-state [[matcher {:keys [params query-params constants]}] state]
  (let [translators (merge params query-params)
        korks (keys translators)
        values (map (fn [key] (let [translator (key translators)
                                   path (conj (:path translator) key)
                                   value (get-in state path)
                                   translated (when-let [tfn (:->route translator)]
                                                (tfn value))]
                               translated)) korks)
        as-map (zipmap korks values)
        route-params (select-keys as-map (keys params))
        query (select-keys as-map (keys query-params))]
    (when (every? identity (vals route-params))
      (let [route (matcher->route matcher route-params)]
        {:route route
         :query query}))))

(defn state->route [state]
  (let [mappings (map #(translate-state % state) @state-mappings)
        route (last (filter identity mappings))]
    route))

(defn translate-param [key translator param]
  (let [path (conj (:path translator) key)
        processor (or (:->state translator) identity)]
    (assoc-in {} path (processor param))))

(defn route->state [{:keys [params query-params constants]
                     :as opts
                     :or {params {}
                          query {}
                          constants {}}} route-params]
  (let [translators (merge params constants query-params)
        route-with-query (merge (dissoc route-params :query-params) (:query-params route-params))
        values (map #(translate-param % (% translators) (% route-with-query)) (keys translators))
        state (apply (partial deep-merge-with merge) values)]
    state))

(defn update-history
  [{:keys [path new-state tag] :as tx-data}]
  (condp = tag
    :silent nil ;ignore
    (let [{:keys [route query]} (state->route new-state)]
      (navigate! route query))))
