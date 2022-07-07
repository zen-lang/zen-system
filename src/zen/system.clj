;; define zen.system protocols
(ns zen.system
  (:require [zen.core :as zen]))

(defn log [ztx ev arg]
  (when-let [a (get-in @ztx [:ctx :logs])]
    (send-off a (fn [_ args] (println :logs (:ev args) (dissoc args :ev))) (assoc arg :ev ev))))

(defn measure [ztx op ev value])

(defmulti start (fn [ztx cmp-def]     (or (:engine cmp-def) (:zen/name cmp-def))))
(defmulti stop  (fn [ztx cmp-def state]     (or (:engine cmp-def) (:zen/name cmp-def))))
(defmulti op    (fn [ztx cmp-def req] (or (:engine cmp-def) (:engine cmp-def) (:zen/name cmp-def))))

(defn call-op [ztx op-name req]
  (if-let [op-def (zen/get-symbol ztx op-name)]
    (op ztx op-def req)
    (println :no-op-def op-name)))

(defmethod start
  :default
  [_ztx cmp-def]
  (println :no-impl (or (:engine cmp-def) (:zen/name cmp-def))))

(defmethod stop
  :default
  [_ztx cmp-def state]
  (println :no-impl (or (:engine cmp-def) (:zen/name cmp-def))))

(defn get-key [ztx model]
  (or (:ctx model) (when-let [tp (:engine model)] (:ctx (zen/get-symbol ztx tp)))))

(defn cmp-stop [ztx op-name]
  (if-let [model (zen/get-symbol ztx op-name)]
    (let [k (get-key ztx model)
          state (and k (get-in @ztx [:ctx k]))]
      (when state
        (log ztx :sys/stop {:op op-name})
        (stop ztx model state)
        (swap! ztx update :ctx dissoc k)))
    (println :no-model op-name)))

(defn cmp-start [ztx op-name]
  (log ztx :sys/start {:op op-name})
  (cmp-stop ztx op-name)
  (if-let [model (zen/get-symbol ztx op-name)]
    (let [k (get-key ztx model)
          state (start ztx model)]
      (when k
        (swap! ztx (fn [ztx]
                     (-> ztx
                         (assoc-in [:ctx k] state)
                         (assoc-in [:start k]  op-name))))))
    (println :no-model op-name)))

(defmethod op
  :default
  [_ztx op-def req]
  (println :no-op-impl (or (:engine op-def) (:zen/name op-def))))

(defn start-system [ztx entry-point]
  (zen/read-ns ztx (symbol (namespace entry-point)))
  (let [system (zen/get-symbol ztx entry-point)]
    (doseq [start-fn (:start system)]
      (cmp-start ztx start-fn))))

(defn stop-system [ztx]
  (doseq [op-name (vals (get-in @ztx [:start]))]
    (cmp-stop ztx op-name)
    (swap! ztx update :start dissoc op-name)))

(defmethod start
  'zen.system/logs
  [_ztx cfg]
  (agent {}))

(defmethod stop
  'zen.system/logs
  [_ztx cfg state])

(defmethod start
  'zen.system/metrics
  [_ztx cfg]
  (agent {}))

(defmethod stop
  'zen.system/metrics
  [_ztx cfg state])
