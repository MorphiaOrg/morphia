+++
title = "Aggregation"
[menu.main]
  parent = "Reference Guides"
  weight = 100
  pre = "<i class='fa fa-file-text-o'></i>"
+++

# Aggregation

The [aggregation](http://docs.mongodb.org/manual/aggregation/) framework in MongoDB allows you to define a series of operations and 
filters (called a pipeline) against the data in a collection.  These pipelines can be used for analytics or they can be used to convert 
your data from one form to another.  This guide will not go in to the details of how aggregation works, however.  The official MongoDB 
[documentation](http://docs.mongodb.org/manual/aggregation/) has extensive tutorials on such details.  Rather, this guide will focus on 
the Morphia API.  The examples shown here are taken from the [tests]({{< srcref 
"morphia/src/test/java/org/mongodb/morphia/aggregation/AggregationTest.java">}}) in Morphia itself.

Writing an aggregation pipeline starts just like writing a standard query.  As with querying, we start with the `Datastore`:

```java
Iterator<Author> aggregate = datastore.createAggregation(Book.class)
      .group("author", grouping("books", push("title")))
      .out(Author.class, options);
```

As with querying, `createAggregation()` takes a `Class` literal.  This lets Morphia know which collection to perform this aggregation 
against.  Because of the transformational operations available in the aggregation [pipeline](http://docs.mongodb
.org/manual/core/aggregation-pipeline/), Morphia can not validate as much as it can with querying so care will need to be taken to ensure
 document fields actual exist when referencing them in your pipeline.  
 
## The Pipeline
Aggregation operations are comprised of a series of steps (or stages) in a pipeline.  Our example here has only the one stage: `group()`.
  This method is the Morphia equivalent of the `$group` operator.  This stage, as the name suggests, groups together documents based on the 
  given field's 
  values.  In this 
  example,
   we are collecting 
  together all the books by authort.
## Options

The `options` reference passed to `out()` above is an instance of [`AggregationOptions`]
(/javadoc/org/mongodb/morphia/aggregation/AggregationOptions.html).  There are a handful options here but there's one that deserves some 
extra attention.  The aggregation pipeline, by default, returns everything "inline" which has the same 16MB limitations as all other 
documents in MongoDB.  As of MongoDB 2.6, you can specify the `$out` pipeline stage to tailor this.  This is what the value of 
[AggregationOptions#getOutputMode()](http://api.mongodb.org/java/3.0/com/mongodb/AggregationOptions.html#getOutputMode--) determines.  The 
 default value is to return them inline but it can be configured to return a cursor instead which means that your result size can be 
much larger than 16MB.
    

### $out

If the aggregation pipeline is a complicated or expensive pipeline that takes quite some time to execute, it can be beneficial to save 
the results of the pipeline for later retrieval. (Think pre-aggregation of traffic statistics, e.g.)  For these cases, the [`$out`]
(http://docs.mongodb.org/manual/reference/operator/aggregation/out/#pipe._S_out) operator was introduced in MongoDB 2.6.  Morphia 
exposes this pipeline stage via the [`out()`](/javadoc/org/mongodb/morphia/aggregation/AggregationPipeline.html#out-java.lang.Class-) method on 
`AggregationPipeline.`Using this pipeline stage, the results can be deposited in to a collection and be 
retrieved at some arbitrary time later.  There is one critical caveat to be aware of,  however.  _**This operation will replace the 
collection contents and any existing data in the collection will be lost.**_  You need to be absolutely sure you give the correct output
 location when using this option because recovery might be difficult.
 
Whether the results are written to a new collection or simply returned as a cursor is controlled by the options you give to Morphia.
 
