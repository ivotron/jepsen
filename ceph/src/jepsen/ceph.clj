(ns jepsen.ceph
  (:require [jepsen.tests :as tests]))

(defn mon-test
  []
  tests/noop-test)
