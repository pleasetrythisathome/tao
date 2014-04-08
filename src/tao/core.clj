(ns tao.core)

(defmacro deftao [name route {:keys [chan params query constants]
                              :as opts
                              :or {params {}
                                   query {}
                                   constants {}}}]
  `(do
     (tao.core/add-state-mapping ~route ~opts)

     (secretary.core/defroute ~name ~route {:as params#}
       (let [key# (cljs.core/keyword '~name)
             translators# (cljs.core/merge ~params ~query ~constants)
             korks# (cljs.core/keys translators#)
             values# (cljs.core/map #(let [path# (cljs.core/conj (cljs.core/get-in translators# [% :path]) %)
                                           processor# (cljs.core/or (cljs.core/get-in translators# [% :->state]) cljs.core/identity)
                                           value# (% params#)]
                                       (cljs.core/assoc-in {} path# (processor# value#)))
                                    korks#)
             state# (cljs.core/apply (cljs.core/partial tao.utils/deep-merge-with cljs.core/merge) values#)]
         (cljs.core.async/put! ~chan [key# state#])))))
