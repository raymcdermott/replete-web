(ns replete.system
  (:require
    [replete.io-impl]
    [replete.fs]
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [replete.editor :as editor]
    [replete.events :as events]))

(defn dev-setup []
  (enable-console-print!))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [editor/replete-editor]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
