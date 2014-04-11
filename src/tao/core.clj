(ns tao.core)

(defmacro deftao [name route {:keys [chan]
                              :as opts}]
  `(do
     (tao.core/add-state-mapping ~route ~opts)

     (secretary.core/defroute ~name ~route {:as params#}
       (let [key# (cljs.core/keyword '~name)
             state# (tao.core/route->state ~opts params#)]
         (cljs.core.async/put! ~chan [key# state#])))))
