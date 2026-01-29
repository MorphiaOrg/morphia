---
title: "Indexing"
weight: 4
description: "Define and manage indexes for MongoDB collections using Morphia annotations at the class and field level."
---

## Indexing

Morphia provides annotations that allow developers to define indexes for a collection to be defined alongside the other mapping data on an entity's source.
In addition to the familiar ascending/descending index types, Morphia and MongoDB support
[TTL](https://docs.mongodb.com/manual/core/index-ttl/), [text](https://docs.mongodb.com/manual/core/index-text/), and [geospatial](https://docs.mongodb.com/manual/applications/geospatial-indexes/)
indexes.
When defining [text indexes](#text-indexing), there are certain restrictions which will be covered below.
Full details for all these types are available in the [manual](https://docs.mongodb.com/manual/indexes).  Morphia will apply any indexes for you at start up by
setting the `morphia.apply-indexes` to true in the [configuration file](/configuration).

There are two ways to define indexes: at the class level and at the field level.

### Class Level Indexes

Class level indexing begins with the [@Indexes](/javadoc/dev/morphia/annotations/Indexes.html) annotation.
This is a container annotation whose sole purpose is to hold a number of [@Index](/javadoc/dev/morphia/annotations/Index.html) annotations.
This annotation has two primary components to cover here:  `fields` and `options`.
An index definition would take the following form:

```java
@Entity
@Indexes({
    @Index(fields = @Field(value = "field2", type = DESC)),
    @Index(
      fields = @Field("field3"),
      options = @IndexOptions(name = "indexing_test")
    )
})
public class IndexExample {
    @Id
    private ObjectId id;
    private String field;
    @Property
    private String field2;
    @Property("f3")
    private String field3;
}
```

### Fields

Which fields to index are defined with the [@Field](/javadoc/dev/morphia/annotations/Field.html) annotation.
An arbitrary number of [@Field](/javadoc/dev/morphia/annotations/Field.html)s can be given but at least one must be present.
The name used for the field can be either the Java field name or the mapped document field name as defined in the class's mapping via, e.g., the
[@Property](/javadoc/dev/morphia/annotations/Property.html) or [@Embedded](/javadoc/dev/morphia/annotations/Embedded.html)
annotations.
For most index types, this value is validated by default.
An exception is made for [text indexing](#text-indexing) as discussed below.

### Index Options

Options for an index are defined on the [@IndexOptions](/javadoc/dev/morphia/annotations/IndexOptions.html).
More complete documentation can be found in the [manual](https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#options).
Using the options allows you to run indexing in the background, e.g. By default, creating an index blocks all other operations on a database.
When building an index on a collection, the database that holds the collection is unavailable for read or write operations until the index build completes.
For potentially long running index building operations, consider the **background** operation so that the MongoDB database remains available during the index building operation.
The MongoDB [manual](https://docs.mongodb.com/manual/core/index-creation/#background-construction) has more detail.

By default Morphia will attempt to validate the fields specified but in some cases that isn't desirable so you can turn it off via the options reference.  `IndexOptions` lets you define [TTL](https://docs.mongodb.com/manual/core/index-ttl/), [sparse](https://docs.mongodb.com/manual/core/index-sparse/), and [partial](https://docs.mongodb.com/manual/core/index-partial/) indexes as well.  `IndexOptions` can also be used to give an index a more human friendly name.

{{% notice note %}}
Whether user specified or MongoDB generated, index names including their full namespace (i.e. database.collection) cannot be longer than the [Index Name Limit](https://docs.mongodb.com/manual/reference/limits/#Index-Name-Length).
{{% /notice %}}

#### Partial Indexes

New in MongoDB 3.2, [partial indexes](https://docs.mongodb.com/v3.2/core/index-partial/) only index the documents in a collection that meet a specified filter expression thereby reducing storage and maintenance costs.
A partial filter is defined using a query as shown here:

```java
@Indexes({@Index(options =
    @IndexOptions(partialFilter = "{ name : { $exists : true } }"),
    fields = {@Field(value = "name")})})
public class SomeClass {
    ...
}
```

## Field Level Indexes

Field level indexing is a simpler approach to defining a basic, single key index.
These indexes are defined by applying the
[@Indexed](/javadoc/dev/morphia/annotations/Indexed.html) annotation to a particular field on a class.
Because the index definition is applied at the field level, the index is created using only that field and so the [@Field](/javadoc/dev/morphia/annotations/Field.html)
annotations are unnecessary.
The options for the index are the same as defined [above](#index-options).
A field level index definition would look like this:

```java
@Entity
private class FieldIndex {
    @Id
    private ObjectId id;
    @Indexed(options = @IndexOptions(unique = true))
    private String name;
    private String color;
}
```

## Text Indexing {#text-indexing}

Morphia's indexing supports MongoDB's text indexing and search functionality as we've briefly seen above.
Full details can be found in the [manual](https://docs.mongodb.com/manual/core/index-text/) but there are a few Morphia specific details to cover.
Indexed field names are validated by default but validation is disabled when an index is defined using MongoDB's
[$**](https://docs.mongodb.com/manual/core/index-text/#text-index-wildcard) syntax.
This special instruction tells MongoDB to create a text index on all fields with string content in a document.
A [compound index](https://docs.mongodb.com/manual/core/index-text/#compound-index) can be created incorporating a text index but it's important to note there can only be one text index on a collection.

A wild card text index declaration would look like this:

```java
    @Indexes(@Index(fields = @Field(value = "$**", type = TEXT)))
```

{{% notice note %}}
A collection can have at most one text index.
{{% /notice %}}

### Collation

Collation allows users to specify language-specific rules for string comparison such as rules for lettercase and accent marks.
A collation can be defined using the `collation()` property on [@IndexOptions](/javadoc/dev/morphia/annotations/IndexOptions.html)
and takes a [@Collation](/javadoc/dev/morphia/annotations/Collation.html) instance.
