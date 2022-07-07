(ns zenbox.db
  (:require [zen.system :as sys]
            [cheshire.core]
            [next.jdbc :as jdbc])
  (:import [org.postgresql.util PGobject]))

(defn coerce-res [res]
  (->> res
       (reduce (fn [acc [k v]]
                 (assoc acc k
                        (if (instance? PGobject v)
                          (cheshire.core/parse-string (.getValue v) keyword)
                          v)))
               {})))

(defn query [ztx query]
  (sys/log ztx :db {:q query})
  (->>
   (jdbc/execute! (get-in @ztx [:ctx :db]) query)
   (mapv coerce-res)))


(defmethod sys/start
  'zenbox/db
  [_ztx cfg]
  (jdbc/get-datasource
   (assoc (:connection cfg) :dbtype "postgres")))

(defmethod sys/stop
  'zenbox/db
  [_ztx cfg state])
