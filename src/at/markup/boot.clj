(ns at.markup.boot
  {:boot/export-tasks true}
  (:require
   [boot.core :as c]
   [me.raynes.conch :refer [with-programs]]
   [clojure.java.io :as io])
  (:import
   [java.io File]
   [org.webjars WebJarAssetLocator]))

(def ^:private webjars-pattern
  #"META-INF/resources/webjars/([^/]+)/([^/]+)/(.*)")

(defn- asset-path [resource]
  (let [[_ name version path] (re-matches webjars-pattern resource)]
    (str name "/" path)))

(defn asset-map
  "Create map of asset path to classpath resource url. Asset path is
  the resource url without webjars part."
  []
  (->> (vals (.getFullPathIndex (WebJarAssetLocator.)))
       (map (juxt asset-path identity))
       (into {})))

(defn- scss->css
  [path]
  (.replaceAll path "\\.scss$" ".css"))

(defn compile-sass
  [sass-file dir]
  (with-programs [sass]
    (let [output (sass "-I" "." "-E" "utf-8" "-t" "compressed" sass-file {:dir dir})]
      output)))

(defn has-anything-changed? [last-fileset fileset]
  (println "has-anything-changed?")
  (let [diff (c/by-ext [".scss"] (c/input-files (c/fileset-diff last-fileset fileset :hash)))
        has-diff (= () diff)]
    (not has-diff)))

(c/deftask sass []
  (let [tmp-css-resource (c/tmp-dir!)
        tmp-scss (c/tmp-dir!)
        last-fileset (atom nil)]
    ;; copy SCSS files from webjars
    (doseq [[k v] (asset-map)
            :when (.endsWith k ".scss")]
      (let [content (slurp (io/resource v))
            out-file (io/file tmp-scss k)]
        (doto out-file io/make-parents (spit content))))
    (fn middleware [next-handler]
      (fn handler [fileset]
        (when (has-anything-changed? @last-fileset fileset)
          (let [in-files (c/input-files fileset)
                scss-files (c/by-ext [".scss"] in-files)]
            ;; copy SCSS files from resources
            (doseq [in scss-files]
              (let [in-file (c/tmp-file in)
                    in-path (c/tmp-path in)
                    out-file (io/file tmp-scss in-path)]
                (doto out-file io/make-parents (spit (slurp in-file)))))
            ;; compile files that do not start with underscores
            (doseq [in scss-files]
              (let [in-file (c/tmp-file in)
                    in-path (c/tmp-path in)
                    out-file (io/file tmp-css-resource (scss->css in-path))]
                (if (not (.startsWith (.getName in-file) "_"))
                  (doto out-file io/make-parents
                        (spit (compile-sass
                               in-path
                               (str tmp-scss)))))))))
        (let [new-fileset (c/commit! (c/add-resource fileset tmp-css-resource))]
          (reset! last-fileset new-fileset)
          (println "new-fileset" new-fileset)
          (next-handler new-fileset))))))
