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
  [sass-file dir options]
  (with-programs [sass]
    (let [output (sass "-I" "." "-E" "utf-8" "-t" (name (:style options)) sass-file {:dir dir})]
      output)))

(defn src-changed? [last-fileset fileset]
  (let [diff (c/by-ext [".scss"] (c/input-files (c/fileset-diff last-fileset fileset :hash)))
        removed (c/by-ext [".scss"] (c/input-files (c/fileset-removed last-fileset fileset)))
        has-diff (not (empty? diff))
        has-removed-files (not (empty? removed))]
    (or has-diff has-removed-files)))

(defn sass-options [options]
  (merge {:style :nested} options))

(c/deftask sass
  [t style VAL kw "Output style. Can be :nested (default), :compact, :compressed, or :expanded."]
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
        (when (src-changed? @last-fileset fileset)
          (c/empty-dir! tmp-css-resource)
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
                               (str tmp-scss)
                               (sass-options *opts*)))))))))
        (let [new-fileset (c/commit! (c/add-resource fileset tmp-css-resource))]
          (reset! last-fileset new-fileset)
          (next-handler new-fileset))))))
