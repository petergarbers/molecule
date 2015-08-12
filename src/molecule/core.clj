(ns molecule.core
  (:gen-class)
  (:import datomic.Util)
  (:require [clojure.java.io :as io]
            [datomic.api :as d]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def conn nil)
(def tempid d/tempid)

(defn connect
  "Connects to database"
  [uri]
  (alter-var-root #'conn (constantly (d/connect uri)))
  conn)

(defn delete-database
  [uri]
  (d/delete-database uri))

(defn db
  "Retrieves a value of the database."
  []
  (d/db conn))

(defn init
  "Creates, initializes, and returns a database value if it does not exist."
  [uri]
  (when (d/create-database uri)
    (connect uri)))

(defn entity
  ([entity-id]
   (entity (db) entity-id))
  ([db entity-id]
   (d/entity db entity-id)))

(defn transact
  ([tx]
   (transact conn tx))
  ([conn tx]
   @(d/transact conn tx)))
