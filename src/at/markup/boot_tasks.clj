(ns at.markup.boot-tasks
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

(c/deftask sass
  []
  (println "sass")
  (let [tmp (c/tmp-dir!)
        tmp-with-webchars (c/tmp-dir!)]
    (fn middleware [next-handler]
      (fn handler [fileset]
        (println "sass handler")
        (c/empty-dir! tmp)
        (println "tmp" tmp)
        (println "tmp-with-webchars" tmp-with-webchars)
        ;; copy SCSS files from webjars
        (doseq [[k v] (asset-map)]
          (if (.endsWith k ".scss")
            (let [content (slurp (io/resource v))
                  out-file (io/file tmp-with-webchars k)]
              (doto out-file io/make-parents (spit content)))))
        (let [in-files (c/input-files fileset)
              scss-files (c/by-ext [".scss"] in-files)]
          (println "sass scss-files" scss-files)
          ;; copy SCSS files from resources
          (doseq [in scss-files]
            (let [in-file (c/tmp-file in)
                  in-path (c/tmp-path in)
                  out-file (io/file tmp-with-webchars in-path)]
              (doto out-file io/make-parents (spit (slurp in-file)))
              (println "scss file:" in-file)))
          ;; compile files that do not start with underscores
          (doseq [in scss-files]
            (let [in-file (c/tmp-file in)
                  in-path (c/tmp-path in)
                  out-file (io/file tmp (scss->css in-path))]
              (println "out-file" out-file)
              (if (not (.startsWith (.getName in-file) "_"))
                (doto out-file io/make-parents
                      (spit (compile-sass in-path (str tmp-with-webchars)))))))
          (next-handler (c/commit! (c/add-resource fileset tmp))))))))
