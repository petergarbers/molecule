(ns molecule.core
  (:gen-class)
  (:import datomic.Util)
  (:require [clojure.java.io :as io]
            [datomic.api :as d]
            [clojure.string :as str]
            [molecule.db :as db]))

(def base-entity-query '[{:find [?e] :with [] :in [$] :where []}])

(def conn (atom nil))

(defn init
  ([db-uri] (init db-uri nil))
  ([db-uri schema-resource] (init db-uri schema-resource nil))
  ([db-uri schema-resource seed-resource]
   (if (d/create-database db-uri)
     (prn "Created database" db-uri)
     (prn "Using existing database" db-uri))
   (let [db-conn (d/connect db-uri)]
     (when schema-resource
       (prn "Loading schema")
       @(d/transact db-conn (read-string (slurp (io/resource schema-resource)))))
     (when seed-resource
       (prn "Loading seed data")
       @(d/transact db-conn (read-string (slurp (io/resource seed-resource)))))
     (reset! conn db-conn))))

(defn db
  ([] (db @conn))
  ([x] (db/as-db x)))

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
  (sequence
   (comp (map entity-key) (distinct) (remove nil?))
   (apply d/datoms db index components)))

(defn- back-ref?
  "Returns true if attr is a back reference; e.g. :a/_b"
  [attr]
  (and (keyword? attr) (namespace attr) (.startsWith (name attr) "_")))

(defn- reverse-attr
  "Reverses the direction of an attribute.
  :a/_b => :a/b
  :a/b => :a/_b"
  [attr]
  (let [name (name attr)]
    (keyword (namespace attr)
             (if (str/starts-with? name "_") (subs name 1) (str "_" name)))))

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

(defn q
  [query db & inputs]
  (apply d/q query db inputs))

(defn query-param
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
  ([attributes] (e (d/db @conn) attributes))
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
         (map first (apply q query)))

       :else
       ()))))

(defn entities
  ([filters] (entities (db) filters))
  ([db filters]
   (->> (e db filters)
        (map #(d/entity db %)))))

(defn entity
  ([entity-id]
   (entity (db) entity-id))
  ([db entity-id]
   (cond
     (or (integer? entity-id) (vector? entity-id)) (d/entity db entity-id)
     (map? entity-id) (first (entities db entity-id)))))
