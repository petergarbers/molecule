# molecule [![Build Status](https://travis-ci.org/petergarbers/molecule.svg?branch=master)](https://travis-ci.org/petergarbers/molecule)


## Installation


```clj

[molecule "0.1.0-SNAPSHOT" :exclusions [com.datomic.datomic-free]] ;; add to your project.clj

```


## Examples

Include in your namespace

```clj

(:require [molecule.core :as m])

```
Connect to your database

```clj

(m/connect db-uri)

```

Fetch an entity

```clj

(m/entity 123123123)


```

Multiple entities by ids

```clj

(m/entities {:db/id [123123123123 3434343434343]}

```

All the entities in a collection

```clj

(db/entities :objects/name)

```

You can traverse backref relationships

```clj

(m/entities {:solar-system/_planets solar-system-id :object/name "Uranus"})

```

If you just need id's

```clj

(m/e {:solar-system/_planets solar-system-id :object/name "Uranus"})

```

### TODO:
Warn if datomic dependency isn't present

Read database uri from ENV

Transact entities 

Use an atom for the connection

FIX https://github.com/petergarbers/molecule/blob/master/test/molecule/core_test.clj#L37

### Thanks

Geoff Catlin 

## License

Copyright © 2016

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
