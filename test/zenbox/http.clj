(ns zenbox.http
  (:require [zen.core :as zen]
            [zen.system :as sys]
            [zenbox.db :as db]
            [cheshire.core]
            [org.httpkit.server :as server]
            [route-map.core :as route-map]))


(defmethod sys/start
  'zenbox/httpkit
  [ztx cfg]
  (server/run-server (fn [req] (sys/call-op ztx (:dispatch cfg) req)) cfg))

(defmethod sys/stop
  'zenbox/httpkit
  [ztx cfg state]
  (state))

(defmulti http-op (fn [ztx cmp-def req] (or (:engine cmp-def) (:zen/name cmp-def))))

(defmethod http-op :default
  [ztx cmp-def req]
  {:status 501
   :body {:message (str "No impl for " (or (:engine cmp-def) (:zen/name cmp-def)))}})

(defmethod http-op 'zenbox/open-api
  [ztx cmp-def req]
  (let [routes (zen/get-symbol ztx (:routes cmp-def))]
    {:body (:routes routes)
     :status 200}))

(defmethod http-op 'zenbox/jsonb-search
  [ztx cmp-def req]
  (let [repo (zen/get-symbol ztx (:repo cmp-def))
        q (format "select * from \"%s\"" (:table repo))
        data (db/query ztx [q])]
    {:body {:data data :query q}
     :status 200}))

(defmethod http-op 'zenbox/jsonb-create
  [ztx cmp-def req]
  (let [repo (zen/get-symbol ztx (:repo cmp-def))
        q (format "insert into \"%s\" (resource) values (?::jsonb) returning *" (:table repo))
        data (db/query ztx [q (cheshire.core/generate-string (:resource req))])]
    {:body {:data data :query q}
     :status 200}))

(defmethod sys/op
  'zenbox/api
  [ztx cmp-def req]
  (sys/log ztx :http {:uri (:uri req) :method (get req :method :get)})
  (let [routes (:routes cmp-def)
        {match :match} (route-map/match [(or (:method req) :get) (:uri req)] routes)]
    (if match
      (if-let [op (when match (zen/get-symbol ztx match))]
        (do
          (sys/log ztx :op {:op match})
          (http-op ztx op req))
        {:status 501 :body {:message (str "No def for " match)}})
      {:status 404 :body {:message (str "No route to " (:method req) " " (:uri req))}})))
