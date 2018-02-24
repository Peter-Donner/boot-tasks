(set-env!
 :source-paths #{"src"}
 :dependencies '[[adzerk/bootlaces "0.1.13" :scope "test"]
                 [me.raynes/conch "0.8.0"]
                 [org.webjars/webjars-locator "0.32-1"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.0.1-SNAPSHOT")
(bootlaces! +version+)

(task-options!
 aot {:namespace '#{at.markup.boot-tasks}}
 pom {:project 'at.markup/boot-tasks
      :version +version+
      :url "https://github.com/Peter-Donner/boot-tasks"
      :scm {:name "git"
            :url "https://github.com/Peter-Donner/boot-tasks"}
      :license {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
