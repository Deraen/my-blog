(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[adzerk/boot-cljs        "0.0-2629-1"]
                  [adzerk/boot-reload      "0.2.3"]
                  [deraen/boot-less        "0.2.0"]
                  [deraen/boot-cryogen     "0.1.0-SNAPSHOT"]
                  [org.webjars/bootstrap   "3.3.1"]
                  [org.webjars/highlightjs "8.4-4"]
                  [pandeiro/boot-http      "0.4.1"]])

(require
  '[adzerk.boot-reload    :refer :all]
  '[adzerk.boot-cljs      :refer :all]
  ; '[cljsjs.app            :refer :all]
  '[deraen.boot-less      :refer :all]
  '[deraen.boot-cryogen   :refer :all]
  '[pandeiro.boot-http    :refer :all])

(task-options!
  less {:source-map true})

(deftask dev []
  (comp
    (serve)
    (watch)
    (reload)
    (repl :server true)
    (cljs :unified-mode true :optimizations :none)
    (less)
    (cryogen)
    (sitemap)))

(deftask deploy []
  (comp
    (cljs :optimizations :advanced)
    (sift :exclude #{"highlight\\..*\\.js"})
    (less :compression true)
    (cryogen)
    (sitemap)
    ; push to github pages
    ))
