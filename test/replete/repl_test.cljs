(ns replete.repl-test
  (:require
    [clojure.test :refer [deftest is testing run-tests]]
    [replete.io-impl]
    [replete.fs]
    [replete.prepl :as prepl]))

(def test-nses
  ["clojure.data"
   "clojure.set"
   "clojure.string"
   "clojure.test.check"
   "clojure.test.check.clojure-test"
   "clojure.test.check.generators"
   "clojure.test.check.properties"
   "clojure.walk"
   "clojure.zip"
   "cljs.pprint"
   "cljs.spec.alpha"
   "cljs.spec.gen.alpha"
   "cljs.spec.test.alpha"
   "cljs.test"])

(defn self-hosted-require
  [ns]
  (first (prepl/read-eval (str "(require '" ns ")"))))

(deftest bundled-cloj-test
  (testing "Verify require on bundled nses"
    (is (= (count test-nses)
           (count (filter
                    (fn [{:keys [tag val] :as r}]
                      (println :result r)
                      (and (= :ret tag) (= "nil" val)))
                    (map self-hosted-require test-nses)))))))



