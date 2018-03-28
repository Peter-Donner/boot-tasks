# boot-tasks
Web development tasks for boot. This library uses spec, so Clojure >= 1.9.0 is needed.

## SASS Compilation

### Requirements
The 'sass' task instruments an existing sass executable (sass on *nix, sass.bat on Windows). So the sass command must work on the shell that is used for the Boot system. If you don't have a sass executable on your system you can use Ruby's version for example.

```
$ gem install sass
```

### Features
- WebJars are supported
- Works well with the 'watch' task
- Sourcemaps are supported

### Options
```
$ boot sass -h

Compile SCSS files.

Options:
  -h, --help                  Print this help info.
  -t, --style VAL             VAL sets output style. Can be :nested (default), :compact, :compressed, or :expanded.
      --sourcemap VAL         VAL sets sourcemap format. Can be :auto (default), :file, :inline, or :none.
  -E, --default-encoding VAL  VAL sets default encoding. utf-8 (default).
```

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
See https://github.com/Peter-Donner/boot-tasks/tree/master/examples/sass-webjars for a working example that supports CSS live reloading.

## WebJars
### webjars task
Work in progress.
```clojure
(require
 '[at.markup.boot :refer [webjars]])
```

```
$ boot [ webjars ls "test.*carousel" ]
```