(ns at.markup.boot
  {:boot/export-tasks true}
  (:require
   [boot.core :as c]
   [me.raynes.conch :refer [with-programs]]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s])
  (:import
   [java.io File]
   [org.webjars WebJarAssetLocator]))

(s/def ::sass-style
  #{:nested :compact :compressed :expanded})

(s/def ::sass-sourcemap
  #{:auto :file :inline :none})

(s/def ::sass-default-encoding
  string?)

(s/def ::sass-options
  (s/keys :req [::sass-style
                ::sass-sourcemap
                ::sass-default-encoding]))

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
  [sass-file dir out-filename options]
  (with-programs [sass]
    (let [output (sass "--load-path" "."
                       "--default-encoding" (::sass-default-encoding options)
                       "--style" (name (::sass-style options))
                       (str "--sourcemap=" (name (::sass-sourcemap options)))
                       sass-file
                       out-filename
                       {:dir dir})]
      output)))

(defn src-changed? [last-fileset fileset]
  (let [diff (c/by-ext [".scss"] (c/input-files (c/fileset-diff last-fileset fileset :hash)))
        removed (c/by-ext [".scss"] (c/input-files (c/fileset-removed last-fileset fileset)))
        has-diff (not (empty? diff))
        has-removed-files (not (empty? removed))]
    (or has-diff has-removed-files)))

(defn sass-options [{:keys [style sourcemap default-encoding]}]
  (let [sass-options
        (s/conform ::sass-options {::sass-style (or style :nested)
                                   ::sass-sourcemap (or sourcemap :auto)
                                   ::sass-default-encoding (or default-encoding "utf-8")})]
    (if (= ::s/invalid sass-options)
      (throw (ex-info "Invalid SASS options" (s/explain-data ::sass-options sass-options)))
      sass-options)))


(c/deftask sass
  "Compile SCSS files."
  [t style VAL kw "output style. Can be :nested (default), :compact, :compressed, or :expanded."
   _ sourcemap VAL kw "sourcemap format. Can be :auto (default), :file, :inline, or :none"
   E default-encoding VAL kw "default encoding. utf-8 (default)"]
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
                (io/make-parents out-file)
                (if (not (.startsWith (.getName in-file) "_"))
                  (compile-sass
                   in-path
                   (str tmp-scss)
                   (.getAbsolutePath out-file)
                   (sass-options *opts*)))))))
        (let [new-fileset (c/commit! (c/add-resource fileset tmp-css-resource))]
          (reset! last-fileset new-fileset)
          (next-handler new-fileset))))))
