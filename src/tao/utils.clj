(ns tao.utils
  (:require [clojure.string :refer [split join]]))

(defn log
  "logs a cljs stuff as js stuff for inspection"
  [& args]
  (.log js/console (clj->js (map clj->js args))))

(defn map->params [query]
  (let [params (map #(name %) (keys query))
        values (vals query)
        pairs (partition 2 (interleave params values))]
   (join "&" (map #(join "=" %) pairs))))

(defn deep-merge-with
  "Like merge-with, but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.

  (deepmerge + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
               {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  -> {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f (if (map? (first maps))
                   maps
                   (rest maps)))))
    maps))
