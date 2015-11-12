(ns molecule.core-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [molecule.core :refer :all]))



(deftest entity-map-test
  (testing "Will create an entity map with the correct db.part"
    (let [em (entity-map :db.part/moo :a :b)
          part (first (:db/id em))]
      (is (= part [:part :db.part/moo])))))
