(ns replete.io-impl
  (:require-macros
    [replete.io-impl :refer [sources]]))

(defn busy-sleep
  [wake-up-time]
  (loop [now (.getTime (js/Date.))]
    (if-not (> now wake-up-time)
      (recur (.getTime (js/Date.))))))

(defn sleep
  [m n]
  (let [now (.getTime (js/Date.))]
    (busy-sleep (+ now m))))

;; Pull all dependencies - we can filter down later
(def dependencies
  (sources chivorcam.core
           clojure.core.reducers
           clojure.data
           clojure.reflect
           clojure.set
           clojure.string
           clojure.walk
           clojure.zip
           clojure.test.check
           clojure.test.check.clojure-test
           clojure.test.check.generators
           clojure.test.check.properties
           clojure.test.check.random
           cljs.analyzer
           cljs.analyzer.api
           cljs.analyzer.macros
           cljs.core
           cljs.env
           cljs.env.macros
           cljs.js
           cljs.nodejs
           cljs.pprint
           cljs.reader
           cljs.repl
           cljs.spec.alpha
           cljs.spec.gen.alpha
           cljs.spec.test.alpha
           cljs.tagged-literals
           cljs.tools.reader
           cljs.tools.reader.reader-types
           cljs.tools.reader.impl.errors
           cljs.tools.reader.impl.commons
           cljs.tools.reader.impl.inspect
           cljs.tools.reader.impl.utils
           cljs.test))

(defn load
  "A namespace loader that looks up the source against the
   given relative path in the dependencies bundle."
  [namespace-relative-path]
  (:source (get dependencies namespace-relative-path)))

(defn load-from-jar
  [file-path resource]
  (throw (ex-info "Unsupported Method"
                  {:method    "REPLETE_LOAD_FROM_JAR"
                   :file-path file-path
                   :resource  resource})))

(defn unsupported-request
  [& args]
  (throw
    (ex-info "Unsupported Method"
             {:method "REPLETE_REQUEST"
              :args   args})))

(set! (.-REPLETE_LOAD js/goog.global) load)
(set! (.-REPLETE_SLEEP js/goog.global) sleep)
(set! (.-REPLETE_LOAD_FROM_JAR js/goog.global) load-from-jar)
(set! (.-REPLETE_REQUEST js/goog.global) unsupported-request)



