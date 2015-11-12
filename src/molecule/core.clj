(ns molecule.core
  (:gen-class)
  (:import datomic.Util)
  (:require [clojure.java.io :as io]
            [datomic.api :as d]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def ^:private base-entity-query '[{:find [?e] :with [] :in [$] :where []}])

(defonce conn nil)
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

(defn entity-map
  ([db-part m] (apply entity-map db-part (apply concat m)))
  ([db-part k v & keyvals]
   {:pre [(instance? clojure.lang.Named db-part)
          (= (namespace db-part) "db.part")
          (even? (count keyvals))]}
   (->> (apply hash-map :db/id (d/tempid db-part) k v keyvals)
        (remove (comp nil? val))
        (into {}))))

(defn- lookup
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
  (try (apply d/q query db inputs)
       (catch Exception e
         ;; TODO: do something
         (throw e))))

(defn query-param
  ;; TODO support additional binding forms
  ;; TODO support functions
  ([query attr]
   (let [back-ref? (back-ref? attr)
         attr (if back-ref? (reverse-attr attr) attr)
         eav (if back-ref? ['_ attr '?e] ['?e attr])]
     (update-in query [0 :where] conj eav)))
  ([query attr val]
   (let [back-ref? (back-ref? attr)
         attr (if back-ref? (reverse-attr attr) attr)
         sym (gensym "?")
         bind (if (coll? val) [sym '...] sym)
         eav (if back-ref? [sym attr '?e] ['?e attr sym])]
     (-> (conj query val)
         (update-in [0 :in] conj bind)
         (update-in [0 :where] conj eav)))))

(defn e
  "Find entity ids based on filters:
  - single attribute, returns all entities with that attribute
  - map of {attr val} pairs
  - map of {attr vals} pairs (where vals is a collection)
  Note: attributes with nil vals or empty collections are ignored."
  ;; TODO support magic :db.value/any value?
  ([filters] (e (d/db conn) filters))
  ([db filters]
   (if (keyword? filters)
     ;; Special case: filters is a keyword
     (let [back-ref? (back-ref? filters)
           attr (if back-ref? (reverse-attr filters) filters)]
       (if back-ref?
         (lookup db :aevt :v attr)
         (lookup db :aevt :e attr)))
     (let [ids? (contains? filters :db/id)
           eids (->> (:db/id filters) values (keep eid))
           filters (->> (dissoc filters :db/id))
           query (cond-> (conj base-entity-query db)
                   (seq eids) (-> (update-in [0 :in] conj '[?e ...])
                                  (conj eids)))]
       (if (or (seq eids) (and (not ids?) (seq filters)))
         (->> (reduce #(apply query-param %1 %2) query filters)
              (apply q)
              (map first))
         ())))))

(defn transact
  ([tx]
   (transact conn tx))
  ([conn tx]
   @(d/transact conn tx)))

(defn retract-entities
  ([entities]
   (retract-entities conn entities))
  ([conn entities]
   (->> (mapv #(list :db.fn/retractEntity (:db/id %)) entities)
        (transact conn))))

(defn retract-entity
  ([entity]
   (retract-entity conn entity))
  ([conn entity]
   (retract-entities conn [entity])))

(defn transact-entities
  ([entities]
   (transact-entities conn entities))
  ([conn entities]
   (let [{:keys [db-after tempids] :as tx} (transact conn entities)]
     (->> (map #(d/resolve-tempid db-after tempids (:db/id %)) entities)
          (map #(d/entity db-after %))))))

(defn transact-entity
  ([entity]
   (transact-entity conn entity))
  ([conn entity]
   (first (transact-entities conn [entity]))))

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
