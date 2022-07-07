(ns zenbox.repo
  (:require [zen.system :as sys]
            [cheshire.core]
            [zen.core :as zen]
            [zenbox.db :as db]))

(defmulti init-repo  (fn [ztx cmp-def opts] (or (:engine cmp-def) (:zen/name cmp-def))))

(defmulti save
  (fn [ztx repo resource & [opts]]
    (or (:engine repo) (:zen/name repo))))

(defmethod save
  :default
  [ztx repo resource & [opts]]
  (throw (Exception. (str "No impl for " (:engine repo)))))

(defmulti search
  (fn [ztx repo query & opts]
    (or (:engine repo) (:zen/name repo))))

(defmethod search
  :default
  [ztx repo query & [opts]]
  (throw (Exception. (str "No impl for " (:engine repo)))))

(defmethod sys/start
  'zenbox/storage
  [ztx cfg]
  (doseq [[k repo-name] (:repos cfg)]
    (when-let [repo (zen/get-symbol ztx repo-name)]
      (init-repo ztx repo {:resourceType k})))
  cfg)

(defmethod sys/start 'zenbox/seeds [_ztx cfg])
(defmethod sys/stop 'zenbox/seeds [_ztx cfg state])

(defmethod sys/start 'zenbox/migrations [_ztx cfg])
(defmethod sys/stop 'zenbox/migrations [_ztx cfg state])

(defmethod sys/stop
  'zenbox/storage
  [_ztx cfg state])

(defmethod init-repo
  :default
  [ztx cmp-def opts] 
  (println :no-init-repo (or (:engine cmp-def) (:zen/name cmp-def)) cmp-def))

(defmethod init-repo
  'zenbox/jsonb-repo
  [ztx cfg opts]
  (let [q (format "create table if not exists \"%s\" (id serial primary key, resource jsonb)" (:table cfg))]
    (sys/log ztx :repo/create-table {:table (:table cfg)})
    (db/query ztx [q])))

(defmethod save
  'zenbox/jsonb-repo
  [ztx repo resource & [opts]]
  (let [q (format "insert into \"%s\" (resource) values (?::jsonb) returning *" (:table repo))
        data (db/query ztx [q (cheshire.core/generate-string resource)])]
    (first data)))

(defmethod search
  'zenbox/jsonb-repo
  [ztx repo query & [opts]]
  (let [q (format "select * from \"%s\"" (:table repo))]
    (db/query ztx [q])))

