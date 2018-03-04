# boot-tasks
Web development tasks for boot.

## SASS Compilation

### Requirements
The 'sass' task instruments an existing sass binary. So the sass command must work on the shell that is used for the Boot system. If you don't have a sass binary on your system you can use Ruby's version for example.

```
$ gem install sass
```

### Features
- WebJars are supported
- Works well with the 'watch' task

### Examples
```clojure
(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http :refer [serve]]
 '[at.markup.boot :refer [sass]])

(deftask dev
  []
  (comp
   (serve :port 8080)
   (watch)
   (reload)
   (sass)
(cljs)))
```
See https://github.com/Peter-Donner/boot-tasks/tree/master/examples/sass for a working example that supports CSS live reloading.