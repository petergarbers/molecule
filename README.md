# molecule

FIXME: description

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar molecule-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

You can get an entity

```clj

(entity 123123123)


```

You can get multiple entities

```clj
(entities {:db/id [123 3434]}

```

You can get all the entities in a collection

```clj

(db/entities :account/users)

```

You can traverse backref relationships

```clj

(entities {:account/_users account-id})

```

You can pass multiple parameters

```clj

(db/entities {:account/_users account-id :user/first-name "bobby" :user/age 19})

```

Transact returns the entity at the end of the transaction




### Bugs

...

### TODO:
Warn if datomic dependency isn't present

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
