(ns molecule.core-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [molecule.core :refer :all]))

(deftest entity-map-test
  (testing "Will create an entity map with the correct db.part"
    (let [em (entity-map :db.part/moo :a :b)
          part (first (:db/id em))]
      (is (= part [:part :db.part/moo]))
      (is (contains? em :a)))))

(deftest query-param-test
  (let [base-query (base-query "db-value" nil)]
    (testing "base query"
      (let [query (query-param base-query :account/name "fudge")]
        (prn query)))))

(deftest e-test
  (with-redefs [d/db (constantly "somedbvalue")
                d/q (fn [a b c] a)]
    (testing "returns empty list if no eids or filters"
      (is (empty? (e2 {})))
      (is (empty? (e2 nil)))
      (is (empty? (e2 {:db/id []}))))

    (testing "if given :db/ids it will return a list of db/ids"
      (is (= [123123 234234] (e2 {:db/id [123123 234234]}))))

    (with-redefs [lookup (fn [db index entity-key & components]
                           [index entity-key components])]
      (testing "find all entities with attribute (no backref)"
        (is (= (e2 :account/name) [:aevt :e '(:account/name)])))

      (testing "find all entities with attribute (with backref)"
        (is (= (e2 :account/_name) [:aevt :v '(:account/name)]))))

    (testing "queries filters and values"
      (with-redefs [gensym (constantly '?123)]
        (is (= (e2 {:dog/sound "woof"})
               {:find ['?e], :with [], :in ['$ '?123], :where [['?e :dog/sound '?123]]}))))))

(deftest real-test
  )
