(defproject tao "0.1.0-SNAPSHOT"
  :description "Two way data binding for browser history"
  :url "http://github.com/pleasetrythisathome/tao"
  :license {:name "Eclipse"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xms512m" "-Xmx512m" "-server"]

  :source-paths  ["src"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [secretary "1.1.0"]
                 [om "0.5.3"]
                 [sablono "0.2.6"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :cljsbuild {
    :builds [{:id "test"
              :source-paths ["src" "test"]
              :compiler {
                :output-to "script/tests.simple.js"
                :output-dir "script/out"
                :source-map "script/tests.simple.js.map"
                :output-wrapper false
                :optimizations :simple}}
             {:id "hello"
              :source-paths ["src" "examples/hello/src"]
              :compiler {
                :output-to "examples/hello/main.js"
                :output-dir "examples/hello/out"
                :source-map true
                :optimizations :none}}]})
