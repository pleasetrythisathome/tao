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
             state# (tao.core/route->state translators# params#)]
         (cljs.core.async/put! ~chan [key# state#])))))
