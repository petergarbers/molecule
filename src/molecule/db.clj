(ns molecule.db
  (:require [datomic.api :as d]))

(defprotocol DatabaseReference
  (as-db [_]))

(extend-protocol DatabaseReference
  datomic.db.Db
  (as-db [db] db)
  datomic.Connection
  (as-db [conn] (d/db conn))
  java.lang.String
  (as-db [dburi] (as-db (d/connect dburi))))
