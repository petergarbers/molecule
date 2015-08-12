(ns molecule.core-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [molecule.core :refer :all]))

(def db-uri "datomic:mem://test")


(defn add-attributes
  []
  (transact [{:db/id #db/id [:db.part/db]
              :db/ident :object/name
              :db/doc "Name of a Solar System object."
              :db/valueType :db.type/string
              :db/index true
              :db/cardinality :db.cardinality/one
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db]
              :db/ident :object/meanRadius
              :db/doc "Mean radius of an object."
              :db/index true
              :db/valueType :db.type/double
              :db/cardinality :db.cardinality/one
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db]
              :db/ident :data/source
              :db/doc "Source of the data in a transaction."
              :db/valueType :db.type/string
              :db/index true
              :db/cardinality :db.cardinality/one
              :db.install/_attribute :db.part/db}])
  (transact [{:db/id #db/id [:db.part/tx]
              :db/doc "Solar system objects bigger than Pluto."}
             {:db/id #db/id [:db.part/tx]
              :data/source "http://en.wikipedia.org/wiki/List_of_Solar_System_objects_by_size"}
             {:db/id #db/id [:db.part/user]
              :object/name "Sun"
              :object/meanRadius 696000.0}
             {:db/id #db/id [:db.part/user]
              :object/name "Jupiter"
              :object/meanRadius 69911.0}
             {:db/id #db/id [:db.part/user]
              :object/name "Saturn"
              :object/meanRadius 58232.0}
             {:db/id #db/id [:db.part/user]
              :object/name "Uranus"
              :object/meanRadius 25362.0}
             {:db/id #db/id [:db.part/user]
              :object/name "Neptune"
              :object/meanRadius 24622.0}
             {:db/id #db/id [:db.part/user]
              :object/name "Earth"
              :object/meanRadius 6371.0}]))

(defn- db-init
  [uri]
  (init uri)
  (add-attributes))

(defn- db-delete
  [uri]
  (delete-database uri))

(deftest entity-test
  (db-init db-uri)
  (prn conn)
  (prn (db))
  (testing "gets an entity by id"

    (d/q '[:find ?e :in $ :where [?e :object/name "Uranus"]]
         (db))
    ;;(.touch (entity (db) 35))
    )
  (db-delete db-uri))
