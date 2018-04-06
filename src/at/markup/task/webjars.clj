(ns at.markup.task.webjars
  (:require
   [boot.core :as c]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [boot.util :refer [print-tree]]
   [at.markup.webjars :refer [asset-map]]))

(s/def ::webjars-cmd-arg
  #{"ls" "export"})

(defn nodes []
  (sort (filter #(not (= ["/"] %))
                (map (fn [x] [x]) (keys (asset-map))))))

(defmulti cmd-handler (fn [[m] _] m))

(defmethod cmd-handler "ls" [[_ regex] fileset]
  (let [regex-nil? (nil? regex)
        label (if regex-nil? "webjars" (str "webjars filter=(" regex ")"))
        nodes (if regex-nil?
                (nodes)
                (filter #(re-find
                          (re-pattern regex) (first %))
                        (nodes)))]
    (print-tree [[label nodes]]))
  fileset)

(defmethod cmd-handler "export" [[_ regex] fileset]
  (let [regex-nil? (nil? regex)
        nodes (if regex-nil?
                (nodes)
                (filter #(re-find
                          (re-pattern regex) (first %))
                        (nodes)))
        assets (asset-map)
        tmp-dir (c/tmp-dir!)]
    (doseq [node nodes]
      (let [v (get assets (first node))
            content (slurp (io/resource v))
            out-file (io/file tmp-dir (first node))]
        (doto out-file io/make-parents (spit content))))
    (c/commit! (c/add-resource fileset tmp-dir))))

(defn webjars
  [args]
  (fn middleware [next-handler]
    (fn handler [fileset]
      (next-handler (cmd-handler args fileset)))))
