# molecule [![Build Status](https://travis-ci.org/petergarbers/molecule.svg?branch=master)](https://travis-ci.org/petergarbers/molecule) [![Clojars Project](https://img.shields.io/clojars/v/molecule.svg)](https://clojars.org/molecule)


## Overview
Molecule is a simple, intuitive wrapper for the datomic library with the goal (for now) of simplying the querying of datomic entities.

## Why should I care?

Have you found yourself writing derivations of this?

```clj
(->> (d/q '[:find ?e
            :in $ ?name
            :where [?e :object/name ?name]]
            db name)
       ffirst
       (d/entity db))

=> {:db/id 17592186045422}

```

OMG:

```
(entity {:object/name name})

=> {:db/id 17592186045422}
```

BUT! What about multiple entities?!
Well that's easy too!

```clj
(->> (d/q '[:find ?e
            :in $ ?name
            :where [?e :object/type :object.type/gas]]
          db name)
     (map #(d/entity db (first %))))

=> ({:db/id 17592186045420} {:db/id 17592186045422})
```

!!!

```clj
(entities {:object/type :object.type/gas})

=> ({:db/id 17592186045420} {:db/id 17592186045422})
```

It does all this and a little more. Please look at the examples below.

## Installation

This should be really easy!

```clj
[molecule "0.1.1-SNAPSHOT"] ;; add to your project.clj
```

Then it's as simple as requiring molecule in the namespaces you'd like to use it from within

```clj
(ns mycoolapp.query
  (:require [molecule.core :as m]))
```

In order for molecule to work we need to initialize our conn atom
I have made this handy-dandy function to help with that.

```clj
(init db-uri)
```

It will also load your schema (and seed data), if you'd like. Both of these args are optional.

```clj
(init db-uri "schema-path.edn" "seed-data.edn")
```

The database value is always at your fingertips.

`(db)`

### Note:
You are by no means forced to use either the `init` or the `db` fn's.
You are welcome to manage your own connection.

All functions accept a database-value as the first argument to the fn's.

```clj
(entities your-db :object.type/gas)
```

## Show me more!

It's good to know that all molecule functions accept the database value as an argument:

```clj
(entity (db) 17592186045420)

=> {:db/id 17592186045420}
```

Well let's start simple. We can fetch an entity using an entity-id

```clj
(entity 17592186045420)

=> {:db/id 17592186045420}
```

Or by multiple entity ids

```clj
(entities {:db/id [17592186045420 17592186045422]}

=> ({:db/id 17592186045420} {:db/id 17592186045422})
```

Sometimes just an entity id is enough.

```clj
(e {:object/type :object.type/gas})

=> (17592186045420 17592186045422)
```

This is where it starts to get useful.
Ever needed all the entities with an attribute?!

```clj
(entities :object/name)

=> ({:db/id 17592186045419} {:db/id 17592186045420} ...)
```

Have you ever needed to work with more complicated entity relationships?
Perhaps a planet within a solar system?
Molecule makes it easy to traverse parent entities.
Let's create a solar system and add some objects.

```clj
@(d/transact @conn [{:db/id (d/tempid :db.part/user)
                                      :solar-system/name "OURS"
                                      :solar-system/planets (e :object/name)}])

=> {:db-before datomic.db.Db@860d79f3, :db-after datomic.db.Db@f6674476, :tx-data [#datom[13194139534334 50 #inst "2017-01-08T19:06:39.509-00:00" 13194139534334 true] #datom[17592186045439 63 "OURS" 13194139534334 true] #datom[17592186045439 64 17592186045419 13194139534334 true] #datom[17592186045439 64 17592186045420 13194139534334 true] ..., :tempids {-9223350046623220537 17592186045439}}

;; Now we can actually do this! And it will only show the objects within our solar system

(entities {:solar-system/_planets 17592186045439 :object/name "Jupiter"})

=> ({:db/id 17592186045420})
```

### TODO:
Warn if datomic dependency isn't present

Transact entities

FIX https://github.com/petergarbers/molecule/blob/master/test/molecule/core_test.clj#L37

### Thanks

Geoff Catlin

## License

Copyright © 2016

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
