(ns examples.hello.core
  (:require [tao.core :as tao :include-macros true]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def app-state (atom {}))

(defn widget [data]
  (om/component
    (dom/div nil "Hello world!")))

(om/root widget app-state
  {:target (.getElementById js/document "app")})
