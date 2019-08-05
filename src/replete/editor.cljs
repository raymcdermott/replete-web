(ns replete.editor
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :refer [h-box v-box box button gap line scroller
                         border label input-text v-split md-icon-button
                         input-textarea title flex-child-style p slider]]
    [replete.cm-edit :as cm-edit]
    [replete.cm-eval :as cm-eval]
    [replete.events :as events]
    [replete.helpers :as helpers]
    [replete.subs :as subs]))

(defn cmirror-eval-box
  "Show results of eval"
  [{:keys [opts cm-opts style]}]
  (let [result (re-frame/subscribe [::subs/eval-result])]
    (fn []
      (let [cm-options (merge {:readOnly true} cm-opts)
            opts0 (merge {:node-id "output" :changes @result} opts)
            eval-opts (assoc opts0 :cm-options cm-options)]
        [box
         :style style
         :child [cm-eval/cmirror-eval-comp eval-opts]]))))

(defn cmirror-edit-box
  "Edit forms with parinfer support"
  [edit-opts cm-opts edit=style]
  (let [clear-form (re-frame/subscribe [::subs/clear-input-form])
        restore-item (re-frame/subscribe [::subs/restore-item])]
    (fn []
      (let [opts0 (merge {:node-id "input"
                          :changes (or @restore-item @clear-form)}
                         edit-opts)
            opts (assoc opts0 :cm-options cm-opts)]
        [box
         :style edit=style
         :child [cm-edit/cmirror-edit-comp opts]]))))

(defn edit-box
  [{:keys [opts cm-opts key-bindings style]}]
  (let [ed-opts (merge {:node-id "input"} opts)
        cmirror-opts (merge {:autofocus true} cm-opts)]
    [v-box :size "100%" :gap "5px"
     :children
     [[cmirror-edit-box ed-opts cmirror-opts style]
      [button
       :class "btn-primary"
       :label (str "Eval")
       :tooltip (str "Shortcut: " (some-> (:enter key-bindings) name))
       :on-click #(re-frame/dispatch [::events/eval])]]]))

(def ^:private replete-preamble-text
  (str "ClojureScript " *clojurescript-version*
       "\n    Docs : (doc function-name)"
       "\n           (find-doc \"part-of-name\")"
       "\n  Source : (source function-name)"
       "\n Results : Stored in *1, *2, *3,"
       "\n           an exception in *e\n\n"))

(def ^:private preamble-markup
  {:start 0
   :end   (helpers/lines-count replete-preamble-text)
   :width (helpers/max-line-width replete-preamble-text)})

(def ^:private main-style
  {:position "absolute"
   :padding  "0px"
   :left     "0px"
   :right    "0px"
   :top      "0px"
   :bottom   "0px"
   :width    "100%"})

(def ^:private editor-style
  (merge (flex-child-style "1")
         {:border        "1px solid lightgrey"
          :border-radius "4px"}))

(def ^:private eval-style
  (flex-child-style "1"))

(defn- key-binding
  [key-map [button event]]
  (assoc {} (get key-map button) #(re-frame/dispatch [event])))

(defn extra-key-bindings
  [key-map event-map]
  (apply merge (map (partial key-binding key-map) event-map)))

(def event-bindings
  {:enter ::events/eval
   :right ::events/history-next
   :down  ::events/history-next
   :left  ::events/history-prev
   :up    ::events/history-prev})

(defn replete-editor
  []
  (let [key-bindings (re-frame/subscribe [::subs/key-bindings])]
    (fn []
      (let [eval-opts {:node-id         "replete-output"
                       :preamble-text   replete-preamble-text
                       :preamble-markup preamble-markup}
            eval-cm-opts {:theme "replete-eval-light"}
            eval-box-opts {:opts    eval-opts
                           :cm-opts eval-cm-opts
                           :style   eval-style}

            extra-keys (extra-key-bindings @key-bindings event-bindings)
            edit-cm-opts {:theme     "replete-edit-light"
                          :extraKeys extra-keys}
            edit-box-opts {:opts         {:node-id "replete-input"}
                           :cm-opts      edit-cm-opts
                           :key-bindings @key-bindings
                           :style        editor-style}]
        [box
         :style main-style
         :child
         [v-split :initial-split 70
          :panel-1 [cmirror-eval-box eval-box-opts]
          :panel-2 [edit-box edit-box-opts]]]))))


