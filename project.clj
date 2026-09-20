(defproject lang/statstrade-v1-slim "0.1.0-SNAPSHOT"
  :description "Standalone Statstrade substrate Currency slice"
  :url "https://github.com/statstrade-dev/v1-slim"
  :license {:name "Proprietary"}
  :aliases {"build-worker" ["run" "-m" "v1-slim.build-worker"]}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [xyz.zcaudate/code.test "4.1.4"]
                 [xyz.zcaudate/js.core "4.1.4"]
                 [xyz.zcaudate/js.react "4.1.4"]
                 [xyz.zcaudate/js.react-ext "4.1.4"]
                 [xyz.zcaudate/js.react-native "4.1.4"]
                 [xyz.zcaudate/lang "4.1.5"]
                 [xyz.zcaudate/std.lib "4.1.4"]
                 [xyz.zcaudate/xtalk.db "4.1.4"]]
  :source-paths ["src" "src-build"]
  :test-paths ["test"]
  :resource-paths ["resources"
                   "src"
                   "src-build"
                   "test"]
  :profiles {:dev {:plugins [[lein-cljfmt "0.7.0"]
                             [cider/cider-nrepl "0.58.0"]]}})
