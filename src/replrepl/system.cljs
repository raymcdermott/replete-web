(ns replrepl.system
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [replrepl.multi :as multi]
    [replete.events :as events]
    [replete.worker-client :as wc]))

(defn dev-setup []
  (enable-console-print!))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [multi/multi-editor]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (wc/init!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
