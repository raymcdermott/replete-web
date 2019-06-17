(ns replrepl.multi
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :refer [h-box v-box box button gap line scroller border
                         label input-text h-split v-split md-icon-button
                         input-textarea title flex-child-style p slider]]
    [replete.events :as events]
    [replete.editor :as editor]
    [replete.subs :as subs]
    [replete.helpers :as helpers]
    [replete.cm-edit :as cm-edit]
    [replete.cm-eval :as cm-eval]))

(def ^:private box-style
  (merge
    (flex-child-style "1")
    {:border        "1px solid lightgrey"
     :border-radius "4px"}))

(defn- enter-binding
  [enter]
  (assoc {} enter #(re-frame/dispatch [::events/eval])))

(defn- up-binding
  [up]
  (assoc {} up #(re-frame/dispatch [::events/history-prev])))

(defn- down-binding
  [down]
  (assoc {} down #(re-frame/dispatch [::events/history-next])))

(defn edit-mirror
  "Edit forms with parinfer support"
  [key-bindings]
  (let [clear-form (re-frame/subscribe [::subs/clear-input-form])
        restore-item (re-frame/subscribe [::subs/restore-item])
        enter (enter-binding (:enter key-bindings))
        down (down-binding (:down key-bindings))
        up (up-binding (:up key-bindings))]
    (fn []
      (let [opts {:node-id    "editor"
                  :changes    (or @restore-item @clear-form)
                  :cm-options {:autofocus true
                               :extraKeys (merge enter up down)
                               :theme     "replete-edit-light"}}]
        [box
         :style box-style
         :child [cm-edit/cmirror-edit-comp opts]]))))

(def ^:private preamble-text
  (str "ClojureScript " *clojurescript-version* "\n\n"))

(def ^:private preamble-markup
  {:start 0
   :end   (helpers/lines-count preamble-text)
   :width (helpers/max-line-width preamble-text)})

(defn eval-mirror
  "Show evalled results from the component it is `watching`"
  []
  (let [result (re-frame/subscribe [::subs/eval-result])]
    (fn []
      (let [opts {:node-id         "eval-history"
                  :cm-options      {:readOnly true
                                    :theme    "replete-eval-light"}
                  :preamble-text   preamble-text
                  :preamble-markup preamble-markup
                  :changes         @result}]
        [box
         :style box-style
         :child [cm-eval/cmirror-eval-comp opts]]))))

(defn edit-panel
  []
  (let [key-bindings (re-frame/subscribe [::subs/key-bindings])]
    (fn []
      [v-box :size "100%" :gap "5px"
       :children
       [[editor/edit-mirror @key-bindings]
        [h-box :gap "5px"
         :align :center
         :children
         [[md-icon-button
           :md-icon-name "zmdi-fast-rewind"
           :style {:color :grey}
           :tooltip-position :below-left
           :tooltip (str "Prev: " (name (:up @key-bindings)))
           :on-click #(re-frame/dispatch [::events/history-prev])]
          [md-icon-button
           :md-icon-name "zmdi-play"
           :style {:color :lightgreen}
           :size :larger
           :tooltip (str "Eval: " (name (:enter @key-bindings)))
           :on-click #(re-frame/dispatch [::events/eval])]
          [md-icon-button
           :md-icon-name "zmdi-fast-forward"
           :style {:color :grey}
           :tooltip-position :right-center
           :tooltip (str "Next: " (name (:down @key-bindings)))
           :on-click #(re-frame/dispatch [::events/history-next])]]]]])))

(defn view-panel
  []
  (let [key-bindings (re-frame/subscribe [::subs/key-bindings])]
    (fn []
      [box :size "100%" :gap "5px"
       :child
       [editor/edit-mirror @key-bindings]])))

(def main-style
  {:position "absolute"
   :padding  "5px"
   :top      "0px"
   :bottom   "0px"
   :width    "100%"})

(defn multi-editor
  []
  [v-split
   :style main-style
   :panel-1 [v-box :width "100%"
             :children
             [[h-split
               :panel-1 [view-panel]
               :panel-2 [eval-mirror]]
              [h-split
               :panel-1 [view-panel]
               :panel-2 [eval-mirror]]]]
   :panel-2 [box :width "100%"
             :child
             [h-split
              :panel-1 [edit-panel]
              :panel-2 [eval-mirror]]]]
  )


