(ns at.markup.boot
  {:boot/export-tasks true}
  (:require
   [boot.core :as c]
   [at.markup.task.sass :as sass-task]))

(c/deftask sass
  "Compile SCSS files."
  [t style VAL kw "output style. Can be :nested (default), :compact, :compressed, or :expanded."
   _ sourcemap VAL kw "sourcemap format. Can be :auto (default), :file, :inline, or :none"
   E default-encoding VAL kw "default encoding. utf-8 (default)"]
  (sass-task/sass *opts*))
