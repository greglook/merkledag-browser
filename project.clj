(defproject mvxcvi/merkledag-browser "0.1.0-SNAPSHOT"
  :description "Clojurescript application to browse the merkledag."
  :url "https://github.com/greglook/merkledag-browser"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :min-lein-version "2.5.3"

  :plugins
  [[lein-figwheel "0.5.0-6"]
   [lein-cljsbuild "1.1.2" :exclusions [[org.clojure/clojure]]]]

  :dependencies
  [[cljs-ajax "0.5.3"]
   [mvxcvi/multihash "1.2.0-SNAPSHOT"]
   [org.clojure/clojure "1.8.0"]
   [org.clojure/clojurescript "1.7.228"]
   [org.clojure/core.async "0.2.374"
    :exclusions [org.clojure/tools.reader]]
   [reagent "0.5.1"]
   [re-frame "0.7.0-alpha-2"]
   [secretary "1.2.3"]]

  :hooks [leiningen.cljsbuild]
  :source-paths ["src"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :hiera
  {:cluster-depth 2
   :vertical false
   :show-external false}

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]

     ;; If no code is to be run, set :figwheel true for continued automagical reloading
     :figwheel {:on-jsload "merkledag.browser.core/on-js-reload"}

     :compiler {:main merkledag.browser.core
                :asset-path "js/compiled/out"
                :output-to "resources/public/js/compiled/merkledag.browser.js"
                :output-dir "resources/public/js/compiled/out"
                :source-map true
                :source-map-timestamp true}}
    ;; This next build is an compressed minified build for
    ;; production. You can build this with:
    ;; lein cljsbuild once min
    {:id "min"
     :source-paths ["src"]
     :compiler {:output-to "resources/public/js/compiled/merkledag.browser.js"
                :main merkledag.browser.core
                :optimizations :advanced
                :pretty-print false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             })
