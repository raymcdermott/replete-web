(ns replete.events
  (:require [clojure.string :as string]
            [re-frame.core :refer [dispatch
                                   reg-event-db
                                   reg-event-fx
                                   reg-fx]]
            [replete.prepl :as prepl]))

(defn- key-bindings
  [os]
  (let [ckey (if (= os :macosx) "cmd" "ctrl")
        keys ["enter" "up" "down" "left" "right"]]
    (into
      {}
      (map
        (fn [c-key the-key]
          [(keyword the-key)
           (keyword (str (string/capitalize c-key)
                         "-"
                         (string/capitalize the-key)))])
        (repeat ckey) keys))))

(defonce ^:private
         os-data
         (let [app-version (.-appVersion js/navigator)
               os (cond
                    (re-find #"Win" app-version) :windows
                    (re-find #"X11" app-version) :unix
                    (re-find #"Linux" app-version) :linux
                    (re-find #"Mac" app-version) :macosx
                    :else :unknown-os)]
           {:os os :key-bindings (key-bindings os)}))

(reg-event-db
  ::initialize-db
  (fn [_ _]
    (merge {:app-name "replete-web"}
           os-data)))

(reg-event-db
  ::save-form
  (fn [db [_ clojure-form]]
    (assoc db :current-form clojure-form
              :clear-input nil)))

(reg-event-db
  ::clear-input
  (fn [db _]
    (assoc db
      :clear-input {:clear-input-form true})))

(reg-event-db
  ::input-history
  (fn [db [_ clojure-form]]
    (let [history (or (:input-history db) [])]
      (assoc db
        :input-history (conj history clojure-form)
        :history-index (-> history count inc)))))

(reg-event-db
  ::eval-result
  (fn [db [_ eval-result]]
    (assoc db :eval-result eval-result)))

(reg-fx
  ::async-eval
  (fn [clojure-form]
    (let [result (prepl/read-eval clojure-form)]
      (dispatch [::eval-result result])
      (dispatch [::input-history clojure-form])
      (dispatch [::clear-input]))))

(reg-event-fx
  ::eval
  (fn [{:keys [db]} _]
    {:db          (assoc db :restore-item nil)
     ::async-eval (:current-form db)}))

(defn- highlight-form
  "Direct codemirror to create a highlight bar over the form.

  Hack #1 Note: `inc` is needed on the `end` line to force codemirror
  to highlight the full width of the last line of the form. Otherwise
  it stops at the end of the text. Let me know if you have a less
  hacky option.

  Hack #2 Note: `.extendSelection` scrolls to `start` but this does
  not always show whole multi-line forms. To make it wholly visible
  a further scroll is attempted with a simple heuristic as an attempt
  to avoid this problem."
  [cm {:keys [start end]}]
  (.extendSelection
    cm #js {:line start :ch 0} #js {:line (inc end) :ch 0})

  (.scrollIntoView
    cm #js {:line (if (< start 15) 0 (+ 2 end))}))

(defn- highlight-history
  [index {:keys [eval-codemirror mark-up-history]}]
  (let [next-mark-up (nth mark-up-history index :not-found)]
    (when-not (= next-mark-up :not-found)
      (highlight-form eval-codemirror next-mark-up))))

(reg-event-db
  ::eval-codemirror
  (fn [db [_ codemirror]]
    (assoc db :eval-codemirror codemirror)))

(reg-event-db
  ::mark-up-history
  (fn [db [_ mark-up-history]]
    (assoc db :mark-up-history mark-up-history)))

(defn- next-prev [db direction-fn]
  (let [index (direction-fn (:history-index db))
        history (:input-history db)
        item (nth history index :not-found)]
    (if (= item :not-found)
      db
      (let [options (select-keys db [:eval-codemirror :mark-up-history])]
        (highlight-history index options)
        (assoc db :history-index index
                  :restore-item item
                  :current-form item)))))

(reg-event-db
  ::history-prev
  (fn [db _]
    (next-prev db dec)))

(reg-event-db
  ::history-next
  (fn [db _]
    (next-prev db inc)))




