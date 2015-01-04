(ns deraen.boot-cryogen
  {:boot/export-tasks true}
  (:require
    [boot.core :as core]
    [boot.util :as util]
    [boot.pod  :as pod]
    [boot.tmpdir :as tmpd]
    [clojure.java.io :as io]
    [clojure.string :as s]))

(def ^:private deps '[[cryogen-core "0.1.11"]])

(defn find-by-root [fileset root]
  (->> fileset core/input-files (core/by-re [(re-pattern (str "^" root))]) (core/by-ext [".md"])))

(defn read-config
  "Reads the config file"
  []
  (-> "templates/config.edn"
      io/resource
      slurp
      read-string
      (update-in [:blog-prefix] (fnil str ""))
      (update-in [:rss-name] (fnil str "rss.xml"))
      (update-in [:post-date-format] (fnil str "yyyy-MM-dd"))))

(defn create-pod []
  (-> (core/get-env)
      (update-in [:dependencies] into deps)
      pod/make-pod
      future))

(defn tmpfile->path [t]
  (-> t tmpd/file .getPath))

(core/deftask cryogen []
  (let [tmp (core/temp-dir!)
        p   (create-pod)]
    (core/with-pre-wrap fileset
      (let [{:keys [post-root page-root] :as config} (read-config)
            posts (find-by-root fileset post-root)
            pages (find-by-root fileset page-root)]
        (pod/with-call-in @p
          (deraen.boot-cryogen.impl/compile-cryogen
            ~(.getPath tmp)
            ~config
            ~(map tmpfile->path posts)
            ~(map tmpfile->path pages)))
      (-> fileset (core/add-resource tmp) core/commit!)))))

(core/deftask sitemap []
  (let [tmp (core/temp-dir!)
        p   (create-pod)]
    (core/with-pre-wrap fileset
      (let [config (read-config)
            html-files (->> fileset core/input-files (core/by-ext [".html"]))]
        (pod/with-call-in @p
          (deraen.boot-cryogen.impl/generate-sitemap
            ~(.getPath tmp)
            ~config
            ~(map tmpfile->path html-files))))
      (-> fileset (core/add-resource tmp) core/commit!))))
