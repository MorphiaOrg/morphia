+++
title = "Querying"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

Morphia offers a fluent API with which to build up a query and map the results back to instances of your entity classes.  It attempts
 to provide as much type safety and validation as possible.  To this end, Morphia offers the __Query<T>__ class which can be parameterized to
the type of your entity.

## Creating a Query

The __Datastore__ is the key class when using Morphia.  Virtually all operations begin with the __Datastore__.  To create the __Query__, we
invoke the following code:

```java
Query<Product> query = datastore.createQuery(Product.class);
```

__createQuery()__ returns an instance of __Query__ with which we can build a query.

### __filter()__

The first method of interest is __filter()__.  This method takes two values: a condition string and a value.  The __value__ parameter is, of
course, the value to use when applying the __condition__ clause.  The __condition__ parameter is a bit more complicated.  At its simplest,
the condition is just a field name.  In this case, the condition is assumed to be an [equality]({{< docsref "reference/operator/query/eq/" >}}) check.  There is a slightly more complicated variant, however.

The __condition__ value can also contain an operator.  For example, to compare a numeric field against a value, you might write something
like this:

```java
query.filter("price >=", 1000);
```

In this case, we're instructing Morphia to add a filter using [$gte]({{< docsref "reference/operator/query/gte/" >}}).  This would result
 in a query that looks like this:

```javascript
{ price: { $gte: 1000 } }
```

The list of supported filter operations can be found in the [FilterOperator]({{< srcref
 "morphia/src/main/java/xyz/morphia/query/FilterOperator.java">}}) class.

| Operator | Alias |
| ---------|------- |
|$center | |
|$centerSphere | |
|$box | |
|$eq | =, == |
|$ne | !=, <> |
|$gt | > |
|$gte | >= |
|$lt | < |
|$lte | <= |
|$exists | exists |
|$type | type |
|$not | |
|$mod | mod |
|$size | size |
|$in | in |
|$nin | nin |
|$all | all |
|$elemMatch | elem, elemMatch |
|$where | |
|$near | near |
|$nearSphere | |
|$within (deprecated replaced by $geoWithin) | within |
|$geoNear | geoNear |
|$geoWithin | geoWithin |
|$geoIntersects | geoIntersects |

 Each filter operator can either be referenced by its MongoDB "dollar operator" or by the aliases listed afterward.  For example, with
 the equal operator, you can use the canonical __$eq__ operator as you would when building a query in the shell or you could opt to use
 either the __=__ or __==__ aliases which might feel a little more natural to use than the dollar operators.

### __field()__

For those who would prefer more compile time validation of their queries, there is __field()__.  This method takes only the field name and
returns an instance of a [class]({{< srcref "morphia/src/main/java/xyz/morphia/query/FieldEnd.java">}}) providing methods with which
 to define your filters.  This approach is slightly more verbose but can be validated by the compiler to a much greater degree than
 __filter()__ can be.  To perform the same query as above, you'd write this:

```java
query.field("price").greaterThanOrEq(1000);
```

This results in the exact same query as the __filter()__ version but has the advantage that any typo in the operation name (method in this
case) would easily be caught by an IDE or compiler.  Which version you use is largely a question of preference.

{{% note %}}
Regardless of the approach used, the field name given can be either the Java field name or the document field name as defined by the
__@Property__ annotation on the field.  Morphia will normalize the name and validate the name such that a query with a bad field name will
result in an error.
{{% /note %}}

## Complex Queries

Of course, queries are usually more complex than single field comparisons.  Morphia offers both __and()__ and __or()__ to build up more
complex queries.  An __and__ query might look something like this:

```java
q.and(
    q.criteria("width").equal(10),
    q.criteria("height").equal(1)
);
```

An __or__ clause looks exactly the same except for using __or()__ instead of __and()__, of course.  For these clauses we use the __criteria()__
method instead of __field()__ but it is used in much the same fashion.  __and()__ and __or()__ take a [__varargs__](https://docs.oracle
.com/javase/8/docs/technotes/guides/language/varargs.html) parameter of type __Criteria__ so you can include as many filters as necessary.
 If all you need is an __and__ clause, you don't need an explicit call to __and()__:

```java
datastore.createQuery(UserLocation.class)
    .field("x").lessThan(5)
    .field("y").greaterThan(4)
    .field("z").greaterThan(10);
```

This generates an implicit __and__ across the field comparisons.

## Text Searching

Morphia also supports MongoDB's text search capabilities.  In order to execute a text search against a collection, the collection must
have a [text index]({{< docsref "/core/index-text/" >}}) defined first.  Using Morphia that definition would look like this:

```java
@Indexes(@Index(fields = @Field(value = "$**", type = IndexType.TEXT)))
public static class Greeting {
    @Id
    private ObjectId id;
    private String value;
    private String language;

    ...
}
```

The __$**__ value tells MongoDB to create a text index on all the text fields in a document.  A more targeted index can be created, if
desired, by explicitly listing which fields to index.  Once the index is defined, we can start querying against it like this [test]({{<
srcref "morphia/src/test/java/xyz/morphia/query/TestTextSearching.java">}}) does:

```java
morphia.map(Greeting.class);
datastore.ensureIndexes();

datastore.save(new Greeting("good morning", "english"),
    new Greeting("good afternoon", "english"),
    new Greeting("good night", "english"),
    new Greeting("good riddance", "english"),
    new Greeting("guten Morgen", "german"),
    new Greeting("guten Tag", "german")),
    new Greeting("gute Nacht", "german"));

List<Greeting> good = datastore.createQuery(Greeting.class)
                             .search("good")
                             .order("_id")
                             .asList();
Assert.assertEquals(4, good.size());
```

As you can see here, we create __Greeting__ objects for multiple languages.  In our test query, we're looking for occurrences of the word
"good" in any document.  We created four such documents and our query returns exactly those four.

## Other Query Options

There is more to querying than simply filtering against different document values.  Listed below are some of the options for modifying
the query results in different ways.

### Projections

[Projections]({{< docsref "tutorial/project-fields-from-query-results/" >}}) allow you to return only a subset of the fields in a
document.  This is useful when you need to only return a smaller view of a larger object.  Borrowing from the [unit tests]({{< srcref
"morphia/src/test/java/xyz/morphia/TestQuery.java" >}}), this is an example of this feature in action:

```java
ContainsRenamedFields user = new ContainsRenamedFields("Frank", "Zappa");
getDs().save(user);

ContainsRenamedFields found = getDs()
    .find(ContainsRenamedFields.class)
    .project("first_name", true)
    .get();
Assert.assertNotNull(found.firstName);
Assert.assertNull(found.lastName);

found = getDs()
    .find(ContainsRenamedFields.class)
    .project("firstName", true)
    .get();
Assert.assertNotNull(found.firstName);
Assert.assertNull(found.lastName);
```

As you can see here, we're saving this entity with a first and last name but our query only returns the first name (and the _id value) in
 the returned instance of our type.  It's also worth noting that this project works with both the mapped document field name
 __"first_name"__ and the Java field name __"firstName"__.

 The boolean value passed in instructs Morphia to either include (__true__) or exclude (__false__) the field.  It is not currently possible to list both inclusions and exclusions in one query.

{{% note class="important" %}}
While projections can be a nice performance win in some cases, it's important to note that this object can not be safely saved back to
 MongoDB.  Any fields in the existing document in the database that are missing from the entity will be removed if this entity is
  saved. For example, in the example above if __found__ is saved back to MongoDB, the __last_name__ field that currently exists in the database
  for this entity will be removed.  To save such instances back consider using [__Datastore#merge(T)__]({{< apiref "xyz/morphia/Datastore#merge-T-" >}})
{{% /note %}}

### Limiting and Skipping

Pagination of query results is often done as a combination of skips and limits.  Morphia offers __Query.limit(int)__ and __Query.offset(int)__
for these cases.  An example of these methods in action would look like this:

```java
datastore.createQuery(Person.class)
    .asList(new FindOptions()
	    .offset(1)
	    .limit(10))
```

This query will skip the first element and take up to the next 10 items found by the query.  There's a caveat to using skip/limit for
pagination, however.  See the [skip]({{< docsref "reference/method/cursor.skip" >}}) documentation for more detail.

### Ordering

Ordering the results of a query is done via [__Query.order(String)__](/javadoc/xyz/morphia/query/Query.html#order-java.lang.String-)
.  The javadoc has complete examples but this String consists of a list of comma delimited fields to order by.  To reverse the sort order
 for a particular field simply prefix that field with a __-__.  For example, to sort by age (youngest to oldest) and then income (highest
 to lowest), you would use this:

```java
query.order("age,-income");
```

### Tailable Cursors

If you have a [capped collection]({{< docsref "core/capped-collections/" >}}) it's possible to "tail" a query so that when new documents
are added to the collection that match your query, they'll be returned by the [tailable cursor]({{< docsref
"reference/glossary/#term-tailable-cursor" >}}).  An example of this feature in action can be found in the [unit tests]({{< srcref
"morphia/src/test/java/xyz/morphia/TestQuery.java">}}) in the __testTailableCursors()__ test:

```java
getMorphia().map(CappedPic.class);
getDs().ensureCaps();                                                          // #1
final Query<CappedPic> query = getDs().createQuery(CappedPic.class);
final List<CappedPic> found = new ArrayList<CappedPic>();

final Iterator<CappedPic> tail = query
	.fetch(new FindOptions()
		.cursorType(CursorType.Tailable));
while(found.size() < 10) {
	found.add(tail.next());                                                    // #2
}
```
There are two things to note about this code sample:

1.  This tells Morphia to make sure that any entity [configured](/guides/annotations/#entity) to use a capped collection has its collection
created correctly.  If the collection already exists and is not capped, you will have to manually [update]({{< docsref
"core/capped-collections/#convert-a-collection-to-capped" >}}) your collection to be a capped collection.
1.  Since this __Iterator__ is backed by a tailable cursor, __hasNext()__ and __next()__ will block until a new item is found.  In this
version of the unit test, we tail the cursor waiting to pull out objects until we have 10 of them and then proceed with the rest of the
application.

### Raw Querying

You can use Morphia to map queries you might have already written using the raw Java API against your objects, or to access features which are not yet present in Morphia.

For example:

```
DBObject query = BasicDBObjectBuilder.start()
	.add("albums",
            new BasicDBObject("$elemMatch",
                    new BasicDBObject("$and", new BasicDBObject[] {
                        new BasicDBObject("albumId", albumDto.getAlbumId()),
                        new BasicDBObject("album",
                            new BasicDBObject("$exists", false))})))
	.get();

Artist result = datastore.createQuery(Artist.class, query).get();
```
