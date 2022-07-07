(ns zenbox.metrics
  (:require [zen.system :as sys]
            [zen.core :as zen]))

(defmethod sys/start
  'zenbox/metrics
  [_ztx cfg]
  (agent {}))

(defmethod sys/stop
  'zenbox/metrics
  [_ztx cfg state])
