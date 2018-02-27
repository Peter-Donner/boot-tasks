(set-env!
 :source-paths   #{"src/scss" "src/cljs"}
 :resource-paths #{"resources"}
 :dependencies '[[adzerk/boot-reload "0.5.2" :scope "test"]
                 [adzerk/boot-cljs "2.1.4" :scope "test"]
                 [at.markup/boot-tasks "0.0.1-SNAPSHOT"]
                 [org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.908"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.webjars.bower/bootstrap "4.0.0" :scope "test"]
                 [pandeiro/boot-http "0.7.6"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http :refer [serve]]
 '[at.markup.boot :refer [sass]])

(deftask dev
  []
  (comp
   (serve :port 8080)
   (watch :verbose true)
   (reload :port 10555 :ws-port 10555)
   (sass)
   (cljs)))
