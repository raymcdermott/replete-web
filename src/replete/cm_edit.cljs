(ns replete.cm-edit
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

(defn- save-changes
  [cm _]
  (let [val (string/trim (.getValue cm))]
    (when (seq val)
      (re-frame/dispatch [::events/save-form val]))))

(defn- update-codemirror
  [cm-ref compnt]
  (when-let [changes (:changes (reagent/props compnt))]
    (.setValue @cm-ref
      (if (:clear-input-form changes) "" changes))))

(defn cmirror-edit-comp
  [opts]
  (let [cmirror (atom nil)
        {:keys [node-id update-fn change-fn cm-options]} opts
        cm-update (partial (or update-fn update-codemirror) cmirror)
        cm-save (or change-fn save-changes)]
    (reagent/create-class
      {:reagent-render
       (fn cm-render
         []
         [:textarea {:id node-id :auto-complete :off}])

       :component-did-mount
       (fn cm-did-mount
         [compnt]
         (let [node (dom/dom-node compnt)
               cm (cm/cm-parinfer node cm-options)]
           (.on cm "change" cm-save)
           (reset! cmirror cm))
         (cm-update compnt))

       :component-will-unmount
       (fn cm-will-unmount
         []
         (.toTextArea @cmirror)
         (reset! cmirror nil))

       :component-did-update
       cm-update

       :display-name
       node-id})))
