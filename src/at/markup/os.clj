(ns at.markup.os)

(defn Windows? []
  (= 0 (.indexOf (System/getProperty "os.name") "Windows")))
