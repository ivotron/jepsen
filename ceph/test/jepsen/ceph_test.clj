(ns jepsen.ceph-test
  (:require [clojure.test :refer :all]
            [jepsen.core :as jepsen]
            [jepsen.ceph :as ceph]))

(deftest mon-test
  (is (:valid? (:results (jepsen/run! (ceph/mon-test))))))
