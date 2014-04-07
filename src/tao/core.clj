(ns tao.core)

(defmacro defstateroute [name route params mappers]
  `(do
     (ccs3.router/add-state-mapping ~route ~mappers)

     (secretary.core/defroute ~name ~route ~params
       (let [key# (cljs.core/keyword '~name)
             translators# (apply cljs.core/merge (vals ~mappers))
             korks# (cljs.core/keys translators#)
             values# (cljs.core/map #(cljs.core/assoc-in {}
                                                        (cljs.core/conj (cljs.core/get-in translators# [% :path]) %)
                                                        ((cljs.core/or (cljs.core/get-in translators# [% :->state]) cljs.core/identity) (cljs.core/get-in translators# [% :symbol])))
                                   korks#)
             state# (cljs.core/apply (cljs.core/partial cljs.core/merge-with cljs.core/merge)
                                    ;; (cljs.core/partial (fn [f & maps]
                                    ;;                      (cljs.core/apply
                                    ;;                       (fn m [& maps]
                                    ;;                         (if (cljs.core/every? map? maps)
                                    ;;                           (cljs.core/apply cljs.core/merge-with m maps)
                                    ;;                           (cljs.core/apply f (if (cljs.core/map? (cljs.core/first maps))
                                    ;;                                                maps
                                    ;;                                                (cljs.core/rest maps)))))
                                    ;;                       maps))
                                    ;;                    cljs.core/merge)
                                     values#)]
         (cljs.core.async/put! ccs3.router/nav-chan [key# state#])))))
