---
title: "Aggregations"
weight: 9
description: "Learn how to use MongoDB's aggregation framework with Morphia to define pipelines and transform data."
---

## Aggregations

The [aggregation framework](https://docs.mongodb.com/manual/aggregation) in MongoDB allows you to define a series (called a pipeline) of operations (called stages) against the data in a collection.
These pipelines can be used for analytics or they can be used to convert your data from one form to another.
This guide will not go in to the details of how aggregation works, however.
The official MongoDB [documentation](https://docs.mongodb.com/manual/aggregation) has extensive tutorials on such details.
Rather, this guide will focus on the Morphia API. The examples shown here are taken from the
[tests](https://github.com/MorphiaOrg/morphia/blob/master/morphia/src/test/java/dev/morphia/aggregation/AggregationTest.java) in Morphia itself. You can find the full list in the [Supported Operators](#supported-operators) section.

Writing an aggregation pipeline starts just like writing a standard query.
As with querying, we start with the `Datastore`:

```java
// Example aggregation code
// See TestDocsExamples.java for full examples
```

`aggregate()` takes a `Class` literal. This lets Morphia know which collection to perform this aggregation against. Because of the transformational operations available in the aggregation [pipeline](https://docs.mongodb.com/manual/core/aggregation-pipeline), Morphia can not validate as much as it can with querying so care will need to be taken to ensure document fields actually exist when referencing them in your pipeline.

### The Pipeline

Aggregation pipelines are comprised of a series stages.
Our example here with the `group()` stage. This method is the Morphia equivalent of the
[$group](https://docs.mongodb.com/manual/reference/operator/aggregation/group/) operator. This stage, as the name suggests, groups together documents based on
various criteria. In this example, we are defining the group ID as the `author` field which will collect all the books by the author
together.

The next step defines a new field, `books` comprised of the titles of the books found in each document.  (For reference, this example is
the Morphia equivalent of an [example](https://docs.mongodb.com/manual/reference/operator/aggregation/group/#group-title-by-author) found in the aggregation tutorials.) This results in a series of documents that look like this:

```json
{ "_id" : "Homer", "books" : [ "The Odyssey", "Iliad" ] }
{ "_id" : "Dante", "books" : [ "The Banquet", "Divine Comedy", "Eclogues" ] }
```

### Executing the Pipeline

Once your pipeline is complete, you can execute it via the `execute()` method.
This method optionally takes a `Class` reference for the target type of your aggregation.
Given this type, Morphia will map each document in the results and return it.
Additionally, you can also include some options to `execute()`.
We can use the various options on the
[AggregationOptions](/javadoc/dev/morphia/aggregation/AggregationOptions.html) class to configure how we want the pipeline to execute.

#### $out

Depending your use case, you might not want to return the results of your aggregation but simply output them to another collection.
That's where `$out` comes in.  [$out](https://docs.mongodb.com/manual/reference/operator/aggregation/out/) is an operator that allows the results of a pipeline to be stored in to a named collection.
This collection can not be sharded or a capped collection, however. This collection, if it does not exist, will be created upon execution of the pipeline.

{{% notice warning %}}
Any existing data in the collection will be replaced by the output of the aggregation.
{{% /notice %}}

An example aggregation using the `$out` stage looks like this:

```java
// Example using $out stage
// See TestDocsExamples.java for full examples
```

You'll note that `out()` is the final stage.  `$out` and `$merge` must be the final stage in our pipeline.  We pass a type to `out()`
that reflects the collection we want to write our output to.  Morphia will use the type-to-collection mapping you've defined when mapping
your entities to determine that collection name.  You may also pass a String with the collection name as well if the target collection
does not correspond to a mapped entity.

#### $merge

[$merge](https://docs.mongodb.com/manual/reference/operator/aggregation/merge/) is a very similar option with a some major differences.
The biggest difference is that `$merge` can write to existing collections without destroying the existing documents.  `$out` would
overwrite any existing documents and replace them with the results of the pipeline.  `$merge`, however, can deposit these new results alongside existing data and update existing data.

Using `$merge` might look something like this:

```java
// Example using $merge stage
// See TestDocsExamples.java for full examples
```

Much like `out()` above, for `merge()` we pass in a collection information but here we are also passing in which database to find/create
the collection in. A merge is slightly more complex and so has more options to consider.
In this example, we're merging in to the `budgets` collection in the `reporting` database and merging any existing documents based on the`_id` as denoted using the `on()` method.
Because there may be existing data in the collection, we need to instruct the operation how to handle those cases.
In this example, when documents matching we're choosing to replace them and when they don't we're instructing the operation to insert the
new documents in to the collection.  Other options are defined on `com.mongodb.client.model.MergeOptions` type defined by the Java driver.

### Supported Operators
Every effort is made to provide 100% coverage of all the operators offered by MongoDB. A select handful of operators have been excluded
for reasons of suitability in Morphia.  In short, some operators just don't make sense in Morphia.  Below is listed all the currently
supported operators.  To see an example of an operator in action, click through to see the test cases for that operator.

If an operator is missing and you think it should be included, please file an [issue](https://github.com/MorphiaOrg/morphia/issues) for that operator.

For detailed information on stages and expressions, see the MongoDB [Aggregation Pipeline Operators documentation](https://docs.mongodb.com/manual/reference/operator/aggregation/).
