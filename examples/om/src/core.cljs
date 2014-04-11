(ns examples.om.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tao.core :refer [deftao]])
  (:require [tao.core :as tao :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [tao.utils :refer [deep-merge-with]]
            [cljs.core.async :refer [<! chan]]))

(enable-console-print!)

(def app-state (atom {:active nil
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
  [{:keys [active sections] :as app} owner]
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
         (map #(om/build button {:data % :app app} {:react-key (:id %)}) sections)]]))))


(def nav-chan (chan))

(deftao section "/:active"
  {:chan nav-chan
   :params {:active {:path []
                     :->state keyword
                     :->route name}}})

;; you should really use push-state and set up
;; your backend to forward all routes to the front end
(tao/init-history {:push-state false})

(om/root app-view app-state
         {:target (.getElementById js/document "app")
          :shared {:navigation nav-chan}
          :tx-listen tao/update-history})
