(ns cryogen-boot.core
  {:boot/export-tasks true}
  (:require
    [boot.core :as core]
    [boot.util :as util]
    [boot.tmpdir :as tmpd]
    [cryogen-core.compiler :as cryogen]
    [selmer.parser :refer [cache-off! render-file]]
    [cryogen-core.io :refer [get-resource]]
    [cryogen-core.sitemap :as sitemap]
    [cryogen-core.rss :as rss]
    [clojure.java.io :as io]
    [clojure.string :as s]
    [text-decoration.core :refer :all]
    [markdown.core :refer [md-to-html-string]]
    [markdown.transformers :refer [transformer-vector]]
    [cryogen-core.toc :refer [generate-toc]]
    [clojure.xml :refer [emit]])
  (:import java.util.Date))

(cache-off!)

(defn path-filter [mkpred]
  (fn [criteria files]
    (let [tmp?   (partial satisfies? tmpd/ITmpFile)
          ->file #(if (tmp? %) (tmpd/path %) (.getPath (io/file %)))]
      (filter #(some identity ((apply juxt (map mkpred criteria)) (->file %))) files))))

(defn by-path-re
  [res files]
  ((path-filter #(fn [p] (re-find % p))) res files))

(defn find-posts
  [fileset {:keys [post-root]}]
  (->> fileset core/input-files (by-path-re [(re-pattern post-root)]) (core/by-ext [".md"]) (map tmpd/file)))

(defn find-pages
  [fileset {:keys [page-root]}]
  (->> fileset core/input-files (by-path-re [(re-pattern page-root)]) (core/by-ext [".md"]) (map tmpd/file)))

(defn read-posts
  "Returns a sequence of maps representing the data from markdown files of posts.
   Sorts the sequence by post date."
  [fileset config]
  (->> (find-posts fileset config)
       (map #(cryogen/parse-page true % config))
       (sort-by :date)
       reverse))

(defn read-pages
  "Returns a sequence of maps representing the data from markdown files of pages.
   Sorts the sequence by post date."
  [fileset config]
  (->> (find-pages fileset config)
       (map #(cryogen/parse-page false % config))
       (sort-by :page-index)))

(defn compile-pages
  "Compiles all the pages into html and spits them out into the public folder"
  [tmp default-params pages {:keys [blog-prefix page-root asset-url]}]
  (when-not (empty? pages)
    (println (blue "compiling pages"))
    (doseq [{:keys [uri] :as page} pages
            :let [f (io/file tmp uri)]]
      (println "\t-->" (cyan uri))
      (io/make-parents f)
      (spit f
            (render-file "templates/html/layouts/page.html"
                         (merge default-params
                                {:servlet-context "../"
                                 :page            page
                                 :asset-url       asset-url}))))))

(defn compile-posts
  "Compiles all the posts into html and spits them out into the public folder"
  [tmp default-params posts {:keys [blog-prefix post-root disqus-shortname asset-url]}]
  (when-not (empty? posts)
    (println (blue "compiling posts"))
    (doseq [post posts
            :let [f (io/file tmp (:uri post))]]
      (println "\t-->" (cyan (:uri post)))
      (io/make-parents f)
      (spit f
            (render-file (str "templates/html/layouts/" (:layout post))
                         (merge default-params
                                {:servlet-context  "../"
                                 :post             post
                                 :disqus-shortname disqus-shortname
                                 :asset-url        asset-url}))))))

(defn compile-tags
  "Compiles all the tag pages into html and spits them out into the public folder"
  [tmp default-params posts-by-tag {:keys [blog-prefix tag-root] :as config}]
  (when-not (empty? posts-by-tag)
    (println (blue "compiling tags"))
    (doseq [[tag posts] posts-by-tag
            :let [{:keys [name uri]} (cryogen/tag-info config tag)
                  f (io/file tmp uri)]]
      (println "\t-->" (cyan uri))
      (io/make-parents f)
      (spit f
            (render-file "templates/html/layouts/tag.html"
                         (merge default-params {:servlet-context "../"
                                                :name            name
                                                :posts           posts}))))))

(defn compile-index
  "Compiles the index page into html and spits it out into the public folder"
  [tmp default-params {:keys [blog-prefix disqus? asset-url]}]
  (println (blue "compiling index"))
  (let [f (io/file tmp blog-prefix "index.html")]
    (io/make-parents f)
    (spit f
          (render-file "templates/html/layouts/home.html"
                       (merge default-params
                              {:home      true
                               :disqus?   disqus?
                               :post      (get-in default-params [:latest-posts 0])
                               :asset-url asset-url})))))

(defn compile-archives
  "Compiles the archives page into html and spits it out into the public folder"
  [tmp default-params posts {:keys [blog-prefix]}]
  (println (blue "compiling archives"))
  (spit (io/file tmp blog-prefix "archives.html")
        (render-file "templates/html/layouts/archives.html"
                     (merge default-params
                            {:archives true
                             :groups   (cryogen/group-for-archive posts)}))))

(defn read-config
  "Reads the config file"
  []
  (-> "templates/config.edn"
      get-resource
      slurp
      read-string
      (update-in [:blog-prefix] (fnil str ""))
      (update-in [:rss-name] (fnil str "rss.xml"))
      (update-in [:post-date-format] (fnil str "yyyy-MM-dd"))))

(core/deftask cryogen []
  (let [tmp (core/temp-dir!)]
    (core/with-pre-wrap
      fileset
      (let [{:keys [site-url blog-prefix recent-posts rss-name] :as config} (read-config)
            posts (cryogen/add-prev-next (read-posts fileset config))
            pages (cryogen/add-prev-next (read-pages fileset config))
            [navbar-pages sidebar-pages] (cryogen/group-pages pages)
            posts-by-tag (cryogen/group-by-tags posts)
            posts (cryogen/tag-posts posts config)
            default-params {:title         (:site-title config)
                            :tags          (map (partial cryogen/tag-info config) (keys posts-by-tag))
                            :latest-posts  (->> posts (take recent-posts) vec)
                            :navbar-pages  navbar-pages
                            :sidebar-pages sidebar-pages
                            :archives-uri  (str blog-prefix "/archives.html")
                            :index-uri     (str blog-prefix "/index.html")
                            :rss-uri       (str blog-prefix "/" rss-name)}]

        (compile-pages    tmp default-params pages config)
        (compile-posts    tmp default-params posts config)
        (compile-tags     tmp default-params posts-by-tag config)
        (compile-index    tmp default-params config)
        (compile-archives tmp default-params posts config)
        (spit (io/file tmp blog-prefix rss-name)      (rss/make-channel config posts)))
      (-> fileset (core/add-resource tmp) core/commit!))))

(defn generate-sitemap [site-url files]
  (with-out-str
    (emit
      {:tag :urlset
       :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
       :content
       (for [f files]
         {:tag :url
          :content
          [{:tag :loc
            :content [(str site-url (sitemap/loc f))]}
           {:tag :lastmod
            :content [(-> f (.lastModified) (Date.) sitemap/format-date)]}]})})))

(core/deftask sitemap []
  (let [tmp (core/temp-dir!)]
    (core/with-pre-wrap
      fileset
      (let [{:keys [site-url blog-prefix] :as config} (read-config)
            html-files (->> fileset core/input-files (core/by-ext [".html"]) (map tmpd/file))]
        (spit (io/file tmp blog-prefix "sitemap.xml")
              (generate-sitemap site-url html-files)))
      (-> fileset (core/add-resource tmp) core/commit!))))
