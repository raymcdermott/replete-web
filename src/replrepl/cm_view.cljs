(ns replrepl.cm-view
  (:require [cljsjs.codemirror]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.hint.show-hint]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.parinfer]
            [cljsjs.parinfer-codemirror]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as re-frame]
            [replete.cm :as cm]
            [replete.events :as events]))

(defn save-form
  [clojure-form]
  (re-frame/dispatch
    [::events/save-form clojure-form]))

(defn enter-binding
  [enter]
  (assoc {} enter #(re-frame/dispatch [::events/eval])))

(defn up-binding
  [up]
  (assoc {} up #(re-frame/dispatch [::events/history-prev])))

(defn down-binding
  [down]
  (assoc {} down #(re-frame/dispatch [::events/history-next])))

(defn cmirror-edit-comp
  [opts]
  (let [cmirror (atom nil)
        node-id (:node-id opts)
        cm-update (fn [comp]
                    (when-let [changes (:changes (reagent/props comp))]
                      (if (:clear-input-form changes)
                        (.setValue @cmirror "")
                        (.setValue @cmirror changes))))]
    (reagent/create-class
      {:reagent-render
       (fn cm-render
         []
         [:textarea {:id node-id :auto-complete :off}])

       :component-did-mount
       (fn cm-did-mount
         [comp]
         (let [node (dom/dom-node comp)
               cm (cm/cm-parinfer node (:cm-options opts))]
           (.on cm "change" (fn [cm _]
                              (let [val (string/trim (.getValue cm))]
                                (when-not (empty? val)
                                  (save-form val)))))
           (reset! cmirror cm))
         (cm-update comp))

       :component-will-unmount
       (fn cm-will-unmount
         []
         (.toTextArea @cmirror)
         (reset! cmirror nil))

       :component-did-update
       cm-update

       :display-name
       node-id})))
