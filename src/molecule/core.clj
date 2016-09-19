(ns molecule.core
  (:gen-class)
  (:import datomic.Util)
  (:require [clojure.java.io :as io]
            [datomic.api :as d]))

(def base-entity-query '[{:find [?e] :with [] :in [$] :where []}])

(defonce conn nil)

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

(defn entity-map
  ([db-part m] (apply entity-map db-part (apply concat m)))
  ([db-part k v & keyvals]
   {:pre [(instance? clojure.lang.Named db-part)
          (= (namespace db-part) "db.part")
          (even? (count keyvals))]}
   (->> (apply hash-map :db/id (d/tempid db-part) k v keyvals)
        (remove (comp nil? val))
        (into {}))))

(defn lookup
  [db index entity-key & components]
  (->> (apply d/datoms db index components)
       (map entity-key)
       distinct
       (remove nil?)))

(defn- back-ref?
  "Returns true if attr is a back reference; e.g. :a/_b"
  [attr]
  (and (keyword? attr) (namespace attr) (.startsWith (name attr) "_")))

(defn- reverse-attr
  "Reverses the direction of an attribute. Intended for ref attributes only.
  For example:
  - given :a/_b, returns :a/b
  - given :a/b, returns :a/_b"
  [attr]
  (let [name (name attr)]
    (keyword (namespace attr)
             (if (.startsWith name "_") (subs name 1) (str "_" name)))))

(defn- lookup-ref?
  [x]
  (let [[attr val & more] (when (sequential? x) x)]
    (and attr val (nil? more)
         (or (integer? attr) (keyword? attr)))))

(defn- eid?
  "Returns true if x is an entity id, keyword, or lookup ref."
  [x]
  (or (integer? x) (keyword? x) (lookup-ref? x)))


(defn entity?
  "Returns true if entity is an EntityMap"
  [entity]
  (instance? datomic.query.EntityMap entity))

(defn eid
  "Coerce to entity id, keyword, lookup ref, or nil if invalid."
  [x]
  (cond
    (eid? x) x
    (or (entity? x) (map? x)) (:db/id x)
    (string? x) (try (Long/valueOf x) (catch Exception e))))

(defn- values
  "Coerces x to a (possibly empty) sequence, if it is not already one.
  If x is a primitive value, yields (list x).  (sequence nil) yields ()"
  [x]
  (cond
    (coll? x) x
    (nil? x) ()
    :else (list x)))

(defn q
  [query db & inputs]
  (apply d/q query db inputs))

(defn query-param
  ;; TODO support additional binding forms
  ;; TODO support functions
  [query attr val]
  (let [back-ref? (back-ref? attr)
        attr (if back-ref? (reverse-attr attr) attr)
        sym (gensym "?")
        bind (if (coll? val) [sym '...] sym)
        eav (if back-ref? [sym attr '?e] ['?e attr sym])]
    (-> (conj query val)
        (update-in [0 :in] conj bind)
        (update-in [0 :where] conj eav))))



(defn base-query
  [db entity-ids]
  (cond-> (conj base-entity-query db)
    (seq entity-ids) (-> (update-in [0 :in] conj '[?e ...])
                         (conj entity-ids))))

(defn e
  ([attributes] (e (d/db conn) attributes))
  ([db attributes]
   (let [eids (:db/id attributes)]
     (cond
       (keyword? attributes)
       (let [lookup-fn (partial lookup db)
             args (if (back-ref? attributes)
                    [:aevt :v (reverse-attr attributes)]
                    [:aevt :e attributes])]
         (apply lookup-fn args))

       ;; Only given eids
       ;; i.e. (entities {:db/id [123123 12344]})
       (and eids (= 1 (count attributes))) eids

       ;; Given attributes to query on
       ;; i.e. (entities {:foo "bar"})
       (seq (dissoc attributes :db/id))
       (let [q-params (dissoc attributes :db/id)
             query (reduce #(apply query-param %1 %2)
                           (conj base-entity-query db)
                           q-params)]
         (prn "query" query)
         (apply q query))

       :else
       ()))))

(defn entity
  ([entity-id]
   (entity (db) entity-id))
  ([db entity-id]
   (d/entity db entity-id)))


(defn entities
  ([filters] (entities (db) filters))
  ([db filters]
   (->> (e db filters)
        (map #(d/entity db %)))))
