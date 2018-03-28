(ns at.markup.task.webjars
  (:require
   [clojure.spec.alpha :as s]
   [boot.util :refer [print-tree]]
   [at.markup.webjars :refer [asset-map]]))

(s/def ::webjars-cmd-arg
  #{"ls"})

(defn nodes []
  (sort (filter #(not (= ["/"] %))
                (map (fn [x] [x]) (keys (asset-map))))))

(defmulti cmd-handler (fn [[m]] m))

(defmethod cmd-handler "ls" [[_ regex]]
  (let [regex-nil? (nil? regex)
        label (if regex-nil? "webjars" (str "webjars filter=(" regex ")"))
        nodes (if regex-nil?
                (nodes)
                (filter #(re-find
                          (re-pattern regex) (first %))
                        (nodes)))]
    (print-tree [[label nodes]])))

(defmethod cmd-handler "export" [cmd-arg]
  (println "export handler"))

(defn webjars
  [args]
  (fn middleware [next-handler]
    (fn handler [fileset]
      (cmd-handler args)
      (next-handler fileset))))
