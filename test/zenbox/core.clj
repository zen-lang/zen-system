(ns zenbox.core
  (:require
   [zen.core :as zen]
   [zen.system :as sys]
   [zenbox.db]
   [zenbox.logs]
   [zenbox.repo]
   [zenbox.metrics]
   [zenbox.http]))


(comment

  (def ztx (zen/new-context {}))

  (zen/read-ns ztx 'myapp)
  (zen/errors ztx)

  (do
    (zen/read-ns ztx 'myapp)
    (sys/start-system ztx 'myapp/system))

  (:ctx @ztx)
  (:start @ztx)

  (sys/stop-system ztx)

  (sys/call-op ztx 'myapp/dispatch {:uri "/"})
  (sys/call-op ztx 'myapp/dispatch {:uri "/Patient"})
  (sys/call-op ztx 'myapp/dispatch {:uri "/Patient" :method :post :resource {:name [{:family "ups"}]}})

  (sys/cmp-start ztx 'myapp/storage)

  (sys/call-op ztx 'myapp/dispatch {:uri "/"})
  (sys/call-op ztx 'zenbox/rpc {:method "myapp/list-tables" :params {}})

  )
