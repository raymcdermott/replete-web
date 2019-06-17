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

(def ^:private editor-box-style
  (merge (flex-child-style "1")
         {:border        "1px solid lightgrey"
          :border-radius "4px"}))

(defn cmirror-box
  "Edit forms with parinfer support"
  [edit-opts cm-opts]
  (let [clear-form (re-frame/subscribe [::subs/clear-input-form])
        restore-item (re-frame/subscribe [::subs/restore-item])]
    (fn []
      (let [opts0 (merge {:node-id "input"
                          :changes (or @restore-item @clear-form)}
                         edit-opts)
            opts (assoc opts0 :cm-options cm-opts)]
        [box
         :style editor-box-style
         :child [cm-edit/cmirror-edit-comp opts]]))))

(defn eval-box
  "Show results of eval"
  [edit-opts cm-opts]
  (let [result (re-frame/subscribe [::subs/eval-result])]
    (fn []
      (let [cm-options (merge {:readOnly true} cm-opts)
            opts0 (merge {:node-id "output"
                          :changes @result}
                         edit-opts)
            opts (assoc opts0 :cm-options cm-options)]
        [box
         :style editor-box-style
         :child [cm-eval/cmirror-eval-comp opts]]))))

(defn edit-box
  [edit-opts cmirror-opts key-bindings]
  (let [ed-opts (merge {:node-id "input"} edit-opts)
        cm-opts (merge {:autofocus true} cmirror-opts)]
    [v-box :size "100%" :gap "5px"
     :children
     [[cmirror-box ed-opts cm-opts]
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
   :padding  "5px"
   :top      "0px"
   :bottom   "0px"
   :width    "100%"})

(defn- key-binding
  [key-map [button event]]
  (assoc {} (get key-map button) #(re-frame/dispatch [event])))

(defn extra-key-bindings
  [key-map event-map]
  (apply merge (map (partial key-binding key-map) event-map)))

(def ^:private event-bindings
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
            edit-opts {:node-id "replete-input"}
            extra-keys (extra-key-bindings @key-bindings event-bindings)
            edit-cm-opts {:theme "replete-edit-light"
                          :extraKeys extra-keys}]
        [box
         :style main-style
         :child [v-split :initial-split 70
                 :panel-1 [eval-box eval-opts eval-cm-opts]
                 :panel-2 [edit-box edit-opts edit-cm-opts @key-bindings]]]))))


