# Tao

Tao enables two-way data binding between edn and browser history.

Tao is a thin wrapper on top of [secretary](https://github.com/gf3/secretary). Although it was designed with [Om](https://github.com/swannodette/om) in mind, but can be used in any system with edn i/o.

## The Way

Routes are created using the ```deftao [route opts]``` macro. Opts is a map containing a channel into which state translated from browser history is pushed, and a params map containing the params matched in the route, their path within the resulting state, and :->state and :->route translation functions.

This simple example that follows will print the route id and the generated state on navigation.

```clojure
(ns examples.basic.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [tao.core :refer [deftao]])
  (:require [tao.core :as tao :include-macros true])))

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
```

browser history can be updated using

```clojure
(tao/update-history new-state)
```

tao will match the last defined route with params in app state that evaluate to true

adding

```clojure
(tao/update-history {:new-state {:section :main}})
```

to the above example will set the browser history to /#/main

## Query and Constants

Query parameters can be defined arbitarily from route params.

```clojure
(deftao section "/:active"
  {:chan nav-chan
   :params {:active {:path []
                     :->state keyword
                     :->route name}}
   :query-params {:search {:path []
                           :->state identity
                           :->route identity}}})
```

Updates to query params do not add to the history stack, and are thus a good way of separating which changes in application state should be treated as steps in navigation.

Constants are additional peices of state that allow you to define additional information associated with a route.


```clojure
(deftao section "/:active"
  {:chan nav-chan
   :params {:active {:path []
                     :->state keyword
                     :->route name}}
   :constants {:status {:path []
                        :->state (constantly nil)}}})
```

Any navigation will result in :status being set to nil

## Om

Integrating with Om is extremely simple

setting tao/update-history as the tx-listen callback in om/root will update history on all state changes

```clojure
;; channel passed into deftao routes
(def nav-chan (chan))

(om/root app-view app-state
         {:target (.getElementById js/document "app")
          :shared {:navigation nav-chan}
          :tx-listen tao/update-history})
```

in app-view

(defn app-view
  [app owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (go
       (while true
         (let [[route state] (<! (om/get-shared owner :navigation))]
           ;; deep-merge navigation state with app-state
           (om/transact! app [] #(tao.utils/deep-merge-with merge % state) :silent)))))
    om/IRender
    (render [_]
      ;; render you app here
      )))

## Examples

To build and run the examples (basic, om)

```shell
cd tao
lein cljsbuild once *example*
cd examples/hello
python -m SimpleHTTPServer
```

Point your browser to localhost:8000.

Clicking the links will update app state. Changes in app state are synced the browser history. Navigating with the back button (or reloading the page) will cause corresponding changes in state.

## Push-state

The code above and the examples use

```clojure
(tao/init-history {:push-state false})
```

With enables hash-bang routes out of convenience and in the interest of keeping the examples simple.

Setting :push-state true (the default value) will use html5 push-state instead of hash-bang routes. This is the prefered choice and requires pushing matching routes from the server to the front end to correctly serve the app on page load. There are all sorts of reasons not to use hash-bang, but if you like, the option is there for you.

## Disclaimer

Tao is very much alpha software. Thoughts, comments, feature, and pull requests welcome.

## License

Copyright © 2014 Dylan Butman

Distributed under the Eclipse Public License.
