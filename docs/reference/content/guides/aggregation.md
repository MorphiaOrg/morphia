+++
title = "Aggregation"
[menu.main]
  parent = "Reference Guides"
  weight = 100
  pre = "<i class='fa fa-file-text-o'></i>"
+++

# Aggregation

The [aggregation framework]({{< docsref "aggregation" >}}) in MongoDB allows you to define a series (called a pipeline) of 
operations (called stages) against the data in a collection.  These pipelines can be used for analytics or they can be used to 
convert your data from one form to another.  This guide will not go in to the details of how aggregation works, however.  The official 
 MongoDB [documentation]({{< docsref "aggregation" >}}) has extensive tutorials on such details.  Rather, this 
 guide will 
 focus on the Morphia API.  The examples shown here are taken from the [tests]({{< srcref 
  "morphia/src/test/java/org/mongodb/morphia/aggregation/AggregationTest.java">}}) in Morphia itself.

Writing an aggregation pipeline starts just like writing a standard query.  As with querying, we start with the `Datastore`:

```java
Iterator<Author> aggregate = datastore.createAggregation(Book.class)
      .group("author", grouping("books", push("title")))
      .out(Author.class, options);
```

`createAggregation()` takes a `Class` literal.  This lets Morphia know which collection to perform this aggregation 
against.  Because of the transformational operations available in the aggregation [pipeline]({{< docsref "core/aggregation-pipeline" >}}),
 Morphia can not validate as much as it can with querying so care will need to be taken to ensure
 document fields actually exist when referencing them in your pipeline.  
 
## The Pipeline
Aggregation operations are comprised of a series stages.  Our example here has only one stage: `group()`.  This method is the Morphia 
equivalent of the [`$group`]({{< docsref "reference/operator/aggregation/group/" >}}) operator.  This stage, as the name 
suggests, groups together documents based on the given field's values.  In this example, we are collecting together all the books by 
author.  The first parameter to `group()` defines the `_id` of the resulting documents.  Within this grouping, this pipeline takes the 
`books` fields for each author and extracts the `title`.  With this grouping of data, we're then `push()`ing the titles in to an array 
in the final document.  This example is the Morphia equivalent of an [example]({{< docsref 
"reference/operator/aggregation/group/#group-title-by-author" >}}) found in the aggregation tutorials.  This results in a series of
 documents that look like this:
 
 ```json
 { "_id" : "Homer", "books" : [ "The Odyssey", "Iliad" ] }
 { "_id" : "Dante", "books" : [ "The Banquet", "Divine Comedy", "Eclogues" ] }
 ```
  
## Executing the Pipeline

There are two basic ways to execute an aggregation pipeline:  `aggregate()` and `out()`.  These methods are Morphia's cues to send the 
 pipeline to MongoDB for execution.  In that regard, both are similar.  In practice, how the results are processed is even very similar. 
  The differences, however, can have huge implications on the performance of your application.  `aggregate()` by default will use the 
 'inline' method for returning the aggregation results.  This approach has the same 16MB limitation that all documents in MongoDB share. 
  We can changes this behavior using the [`AggregationOptions`](http://api.mongodb.org/java/3.0/com/mongodb/AggregationOptions.html) 
  class.  The `options` reference we passed to `out()` also applies to `aggregate()`.

### Aggregation Options

There are a handful options here but there's one that deserves some extra attention. As mentioned, the aggregation pipeline, by default,
 returns everything "inline" but as of MongoDB 2.6 you can tell the aggregation framework to return a cursor instead.  This is what the 
 value of [AggregationOptions#getOutputMode()](http://api.mongodb.org/java/3.0/com/mongodb/AggregationOptions.html#getOutputMode--) 
 determines.  By setting the output mode to `CURSOR`, MongoDB can return a result size much larger than 16MB.  The options can also be 
 configured to update the batch size or to set the time out threshold after which an aggregation will fail.  It is also possible to tell
  the aggregation framework to use disk space which allows, among other things, sorting of larger data sets than what can fit in memory 
  on the server.
    
### $out

But this example doesn't use `aggregate()`, of course, it uses `out()` which gives us access to the `$out` pipeline stage.  [`$out`]
({{< docsref "reference/operator/aggregation/out/" >}}) is a new operator in MongoDB 2.6 that allows the results of a 
pipeline to be stored in to a named collection.  This collection can not be sharded or a capped collection, however.  This collection, 
if it does not exist, will be created upon execution of the pipeline.  

{{% note class="important" %}}
Any existing data in the collection will be replaced by the output of the aggregation.
{{% /note %}}

Using `out()` is implicitly asking for the results to be returned via a cursor.  What is happening under the covers is the aggregation 
framework is writing out to the collection and is done.  Morphia goes one extra step further and executes an implicit `find` on the output 
collection and returns a cursor for all the documents in the collection.  In practice, this behaves no differently than setting the 
output mode to `CURSOR` with `aggregate()` and your application need not know the difference.  It does, of course, have an impact on your 
database and any existing data.  The use of `$out` and `out()` can be greatly beneficial in scenarios such as precomputed aggregated 
results for later retrieval.

### Typed Results

`out()` has several variants.  In this example, we're passing in `Author.class` which tells Morphia that we want to map each document 
returned to an instance of `Author`.  Because we're using `out()` instead of `aggregate()`, Morphia will use the mapped collection for 
`Author` as the output collection for the pipeline.  If you'd like to use an alternate collection but still return a cursor of `Author` 
instances, you can use [`out(String,Class,AggregationOptions)`](/javadoc/org/mongodb/morphia/aggregation/AggregationPipeline.html#out-java
.lang.String-java.lang.Class-com.mongodb.AggregationOptions-) instead.