(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[; FIXME:
                  [cryogen-core "0.1.9"]

                  [adzerk/boot-cljs        "0.0-2411-8"]
                  [adzerk/boot-reload      "0.2.2"]
                  ; [cljsjs/boot-cljsjs      "0.3.0"]
                  [deraen/boot-less        "0.2.0"]
                  [org.webjars/bootstrap   "3.3.1"]
                  [org.webjars/highlightjs "8.4"]
                  [pandeiro/boot-http      "0.4.0"]])

(require
  '[adzerk.boot-reload    :refer :all]
  '[adzerk.boot-cljs      :refer :all]
  ; '[cljsjs.app            :refer :all]
  '[deraen.boot-less      :refer :all]
  '[cryogen-boot.core     :refer :all]
  '[pandeiro.http         :refer :all])

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
