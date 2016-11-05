(ns molecule.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [datomic.api :as d]
            [molecule.core :refer :all])
  (:import datomic.Util))

(def db-uri-base "datomic:mem://")

(defn scratch-conn
  []
  (let [uri (str db-uri-base (d/squuid))]
    (d/delete-database uri)
    (d/create-database uri)
    (d/connect uri)))

(def test-conn (scratch-conn))

(defn read-all
  "Read all forms in f, where f is any resource that can
   be opened by io/reader"
  [f]
  (Util/readAll (io/reader f)))

(defn transact-all
  "Load and run all transactions from f, where f is any
   resource that can be opened by io/reader."
  [conn f]
  (loop [n 0
         [tx & more] (read-all f)]
    (if tx
      (recur (+ n (count (:tx-data  @(d/transact conn tx))))
             more)
      {:datoms n})))

(transact-all test-conn (io/resource "molecule/schema.edn"))

(deftest entity-map-test
  (testing "Will create an entity map with the correct db.part"
    (let [em (entity-map :db.part/moo :a :b)
          part (first (:db/id em))]
      (is (= part [:part :db.part/moo]))
      (is (contains? em :a)))))

(deftest query-param-test
  (let [base-query (base-query "db-value" nil)]
    (testing "base query"
      (let [query (query-param base-query :object/name "fudge")]
        (prn query)))))

(deftest e-test
  (prn "AAAA")
  (with-redefs [d/db (constantly "somedbvalue")
                d/q (fn [a b c] a)]
    (testing "returns empty list if no eids or filters"
      (is (empty? (e {})))
      (is (empty? (e nil)))
      (is (empty? (e {:db/id []}))))

    (testing "if given :db/ids it will return a list of db/ids"
      (is (= [123123 234234] (e {:db/id [123123 234234]}))))

    (with-redefs [lookup (fn [db index entity-key & components]
                           [index entity-key components])]
      (testing "find all entities with attribute (no backref)"
        (is (= (e :object/name) [:aevt :e '(:object/name)])))

      (testing "find all entities with attribute (with backref)"
        (is (= (e :object/_name) [:aevt :v '(:object/name)]))))

    #_(testing "queries filters and values"
      (with-redefs [gensym (constantly '?123)]
        (is (= (e {:dog/sound "woof"})
               {:find ['?e], :with [], :in ['$ '?123], :where [['?e :dog/sound '?123]]}))))))

(deftest real-transactions-test
  (let [db (d/db test-conn)]
    (testing "can find an entity by the id"
      (let [uranus-id (ffirst (d/q '[:find ?e
                                     :where
                                     [?e :object/name "Uranus"]]
                                   db))]
        (is (entity? (first (entities db {:db/id [uranus-id]}))))
        (is (= uranus-id (:db/id (first (entities db {:db/id [uranus-id]})))))
        (is (entity? (entity db uranus-id)))))
    (testing "can find an entity by attributes"
      (is (= "Uranus"
             (:object/name (first (entities db {:object/name "Uranus"}))))))))
