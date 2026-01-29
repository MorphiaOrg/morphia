---
title: "Quick Tour"
weight: 2
description: "A quick tour of Morphia's features"
---

# Quick Tour

Morphia wraps the MongoDB Java driver so some level of familiarity with using the driver can be helpful. Morphia does its best to abstract much of that away but if something is confusing, please consult the Java driver [documentation](http://mongodb.github.io/mongo-java-driver/) as well.

## Setting up Morphia

The following example shows how to create the initial Morphia instance. Using this instance, you can configure various aspects of how Morphia maps your entities and validates your queries.

```java
final Datastore datastore = Morphia.createDatastore(MongoClients.create());
```

This snippet creates the Morphia instance we'll be using in our simple application. The `Morphia` class is a factory for `Datastore` instances. Given this minimal setup, Morphia will use a database named `morphia` and will scan your entire classpath for entities to map.

This default setup will connect to the mongod running on the local machine using the default port of `27017`. When Morphia scans looking for entities, it will look for classes annotated with `@Entity` and register the mapping metadata we've put on our classes.

## Mapping Options

You can configure various mapping options via the `MorphiaConfig` class. There are a number of items to configure here but for now we'll just cover two. Two common elements to configure are probably `storeEmpties` and `storeNulls`. By default Morphia will not store empty `List` or `Map` values nor will it store null values into MongoDB. If your application needs empty or null values to be present for whatever reason, setting these values to true will tell Morphia to save them for you.

## Mapping Classes

There are two ways that Morphia can handle your classes: as top level entities or embedded in others. Any class with `@Entity` must have a field annotated with `@Id` to define which field to use as the `_id` value in the document written to MongoDB if the entity is to be stored as a top level type and not just used as property type on another entity.

{{% notice note %}}
In order to be considered for persistence by Morphia, classes **must** be annotated with either `@Entity` or `@ExternalEntity`. Classes lacking either of these annotations will effectively be ignored by Morphia.
{{% /notice %}}