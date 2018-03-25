(ns at.markup.task.webjars
  (:require
   [clojure.spec.alpha :as s]
   [boot.util :refer [print-tree]]
   [at.markup.webjars :refer [asset-map]]))

(s/def ::webjars-cmd-arg
  #{"list"})

(defn webjars
  [[cmd]]
  (fn middleware [next-handler]
    (fn handler [fileset]
      (let [nodes (sort (filter #(not (= ["/"] %))
                                (map (fn [x] [x]) (keys (asset-map)))))]
        (print-tree [["webjars" nodes]])
        (next-handler fileset)))))
