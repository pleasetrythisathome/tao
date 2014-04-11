(ns examples.basic.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tao.core :refer [deftao]])
  (:require [tao.core :as tao :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<! chan]]))

(enable-console-print!)

(def nav-chan (chan))

(deftao section "/:section"
  {:chan nav-chan
   :params {:section {:path []
                     :->state keyword
                     :->route name}}})

(go
 (while true
   (let [[route state] (<! nav-chan)]
     (print route state))))

(tao/init-history {:push-state false})

(tao/update-history {:new-state {:section :main}})
