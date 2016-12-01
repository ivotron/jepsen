(ns jepsen.ceph
  (:require [clojure.string :as str]
            [jepsen [db         :as db]
                    [checker    :as checker]
                    [client     :as client]
                    [control    :as c]
                    [generator  :as gen]
                    [model      :as model]
                    [nemesis    :as nemesis]
                    [tests      :as tests]
                    [util :refer [timeout]]]
	     [knossos.model      :as checkModel]
))


(defn r [_ _] {:type :invoke, :f :read, :value nil})
(defn w [_ _] {:type :invoke, :f :write, :value (rand-int 5)})

(defn mon-set!
  "Set a value for key"
  [node k v]
  (c/on node
     (c/exec :ceph :config-key :put k v)))

(defn mon-get!
  "Get a value for key"
  [node k]
  (c/on node
     (c/exec :ceph :config-key :get k :-o :v)
     (c/exec :cat :v)))

(defn client
  "A client for a get/set register"
  [node k]
  (reify client/Client
    (setup! [_ test node]
      (let [n node k "keyRegister"] (client n k)))

    (invoke! [this test op]
      (timeout 5000 (assoc op :type :info, :error :timeout)
        (case (:f op)
          :read  (let[resp (-> (mon-get! node k))]
                   (assoc op :type :ok, :value resp))
          ;:write ((mon-set! node k (:value op))
	  :write (let[resp(-> (mon-set! node k (:value op)))]
                  (assoc op :type :ok, :value resp))
	)))

    (teardown! [_ test])))

(defn mon-test
  []
  (assoc tests/noop-test
     :name "cephmon"
     :nemesis (nemesis/partition-random-halves)
     :client (client nil nil) 
     :generator (->> (gen/mix [r w])
                     (gen/stagger 1)
                     (gen/nemesis
                       (gen/seq (cycle [(gen/sleep 5)
                                        {:type :info, :f :start}
                                        (gen/sleep 5)
                                        {:type :info, :f :stop}])))
                     (gen/time-limit 45))
     ;:model (model/set)
     :checker checker/linearizable
))
