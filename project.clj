(defproject lms "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.0"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [io.pedestal/pedestal.service "0.4.0"]

                 ;; Remove this line and uncomment one of the next lines to
                 ;; use Immutant or Tomcat instead of Jetty:
                 [io.pedestal/pedestal.jetty "0.4.1"]
                 ;; [io.pedestal/pedestal.immutant "0.4.1"]
                 ;; [io.pedestal/pedestal.tomcat "0.4.1"]

                 [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.12"]
                 [org.slf4j/jcl-over-slf4j "1.7.12"]
                 [org.slf4j/log4j-over-slf4j "1.7.12"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [domina "1.0.3"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [com.novemberain/monger "2.1.0"]
                 [selmer "0.9.5"]
                 [markdown-clj "0.9.80"]
                 [environ "1.0.1"]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [ring/ring-defaults "0.1.5"]
                 [com.cognitect/transit-cljs "0.8.232"]
                 [cheshire "5.5.0"]
                 [com.cognitect/transit-cljs "0.8.232"]
                 [clojurewerkz/scrypt "1.2.0"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [cljs-ajax "0.3.14"]
                 [com.cemerick/url "0.1.1"]
                 [clj-http "2.0.0"]]
  :cljsbuild
 {:builds
 [
   {:compiler
     {:output-to "../lms/resources/public/js/client.js",
      :optimizations :advanced,
      :pretty-print true},
     :source-paths ["src/lms/cljs"]}]}
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "lms.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.4.1"]]}
             :uberjar {:aot [lms.server]}}
  :main ^{:skip-aot true} lms.server)
