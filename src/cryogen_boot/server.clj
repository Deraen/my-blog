(ns cryogen-boot.server
  {:boot/export-tasks true}
  (:require
    [boot.pod           :as pod]
    [boot.util          :as util]
    [boot.core          :as core]
    [boot.task.built-in :as task]))

(def ^:private deps
  '[[ring/ring-jetty-adapter "1.3.1"]
    [compojure "1.2.1"]])

(core/deftask serve
  "Start a web server on localhost and serve a directory, blocking
   the boot task pipeline by default.
   If no directory is specified the current one is used.  Listens
   on port 3000 by default."
  [d dir      PATH     str  "The directory to serve."
   p port     PORT     int  "The port to listen on."]
  (let [worker   (pod/make-pod (assoc-in (core/get-env) [:dependencies] deps))
        dir      (or dir "")
        port     (or port 3000)]
    (core/cleanup
      (util/info "\n<< stopping Jetty... >>\n")
      (pod/with-eval-in worker (.stop server)))
    (core/with-pre-wrap
      fileset
      (pod/with-eval-in
        worker
        (require '[ring.adapter.jetty :refer [run-jetty]]
                 '[compojure.core     :refer [routes GET]]
                 '[compojure.route    :refer [resources]]
                 '[ring.util.response :refer [resource-response]])
        (def server
          (run-jetty
            (routes (GET "/" []
                      (resource-response "index.html" {:root ~dir}))
                    (resources "/" {:root ~dir}))
            {:port ~port :join? false})))
      (util/info "<< started Jetty on http://localhost:%d (serving: %s) >>\n" port dir)
      fileset)))
