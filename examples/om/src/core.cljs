(ns examples.om.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tao.core :refer [deftao]])
  (:require [tao.core :as tao :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [tao.utils :refer [deep-merge-with]]
            [cljs.core.async :refer [<! chan]]))

(enable-console-print!)

(def app-state (atom {:active :admin
                      :search ""
                      :sections [{:id :admin
                                  :title "Admin"}
                                 {:id :main
                                  :title "Main"}
                                 {:id :other
                                  :title "Other"}]}))

(defn button
  [{{:keys [id title]} :data {:keys [active] :as app} :app} owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div {:class "button"
              :on-click #(om/update! app :active id)}
        (if (= active id)
          (str title "<")
          title)]))))

(defn app-view
  [{:keys [active search sections] :as app} owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (go
       (while true
         (let [[route state] (<! (om/get-shared owner :navigation))]
           (om/transact! app [] #(deep-merge-with merge % state) :silent)))))
    om/IRender
    (render [_]
      (html
       [:div {:class "app"}
        [:div
         (map #(om/build button {:data % :app app} {:react-key (:id %)}) sections)]
        [:input {:value search
                 :on-change #(om/update! app :search (.-value (.-target %)))}]]))))


(def nav-chan (chan))

(deftao home "/"
  {:chan nav-chan
   :constants {:active {:path []
                        :->state (constantly :admin)}
               :search {:path []
                        :->state (constantly "")}}})

(deftao section "/:active"
  {:chan nav-chan
   :params {:active {:path []
                     :->state keyword
                     :->route name}}
   :query-params {:search {:path []
                           :->state identity
                           :->route identity}}})

;; you should really use push-state and set up
;; your backend to forward all routes to the front end
(tao/init-history {:push-state false})

(om/root app-view app-state
         {:target (.getElementById js/document "app")
          :shared {:navigation nav-chan}
          :tx-listen tao/update-history})
