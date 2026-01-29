---
title: "Queries"
weight: 6
description: "Learn how to build queries in Morphia to find and filter documents with type-safe operations"
---

## Queries

Morphia provides [Query<T>](/javadoc/dev/morphia/query/Query.html) class to build a query and map the results back to instances of your entity classes and attempts to provide as much type safety and validation as possible.
To create the `Query`, we invoke the following code:

```java
Query<Product> query = datastore.find(Product.class);
```

`find()` returns an instance of `Query` which we can use to build a query.

### `filter()`

The most significant method `filter(Filter...)`.
This method takes a number of filters to apply to the query being built.
The filters are added to any existing, previously defined filters so you needn't add them all at once.
There are dozens of filters predefined in Morphia and can be found in the `dev.morphia.query.filters` package.  You can find the full
list in the [Supported Operators](#supported-operators) section.

The filters can be accessed via the [Filters](/javadoc/dev/morphia/query/filters/Filters.html) class.
The method names largely match the operation name you would use querying via the mongo shell so this should help you translate queries in to Morphia's API.
For example, to query for products whose prices is greater than or equal to 1000, you would write this:

```java
query.filter(Filters.gte("price", 1000));
```

This will append the new criteria to any existing criteria already defined.
You can define as many filters in one call as you'd like or you may choose to append them in smaller groups based on whatever query building logic your application might have.

### Complex Queries

Of course, queries are usually more complex than single field comparisons.
Morphia offers both `and()` and `or()` to build up more complex queries.
An `and` query might look something like this:

```java
q.filter(and(
    eq("width", 10),
    eq("height", 1)));
```

An `or` clause looks exactly the same except for using `or()` instead of `and()`, of course.
The default is to "and" filter criteria together so if all you need is an `and` clause, you don't need an explicit call to `and()`:

```java
datastore.find(UserLocation.class)
    .filter(
        lt("x", 5),
        gt("y", 4),
        gt("z", 10));
```

This generates an implicit `and` across the field comparisons.

## Other Query Options

There is more to querying than simply filtering against different document values.
Listed below are some of the options for modifying the query results in different ways.

### Projections

[Projections](https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/) allow you to return only a subset of the fields in a document.
This is useful when you need to only return a smaller view of a larger object.
Borrowing from the
[unit tests](https://github.com/MorphiaOrg/morphia/blob/master/morphia/src/test/java/dev/morphia/TestQuery.java), this is an example of this feature in action:

```java
ContainsRenamedFields user = new ContainsRenamedFields("Frank", "Zappa");
datastore.save(user);

ContainsRenamedFields found = datastore
                                  .find(ContainsRenamedFields.class)
                                  .iterator(new FindOptions()
                                               .projection().include("first_name")
                                               .limit(1))
                                  .tryNext();
assertNotNull(found.firstName);
assertNull(found.lastName);
```

As you can see here, we're saving this entity with a first and last name but our query only returns the first name (and the `_id` value) in the returned instance of our type.
It's also worth noting that this projection works with both the mapped document field name
`"first_name"` and the Java field name `"firstName"`.

{{% notice warning %}}
While projections can be a nice performance win in some cases, it's important to note that this object can not be safely saved back to MongoDB. Any fields in the existing document in the database that are missing from the entity will be removed if this entity is saved.
For example, in the example above if `found` is saved back to MongoDB, the `last_name` field that currently exists in the database for this entity will be removed.
To save such instances back consider using
[Datastore#merge(T)](/javadoc/dev/morphia/Datastore.html#merge(T))
{{% /notice %}}

### Limiting and Skipping

Pagination of query results is often done as a combination of skips and limits.
Morphia offers `FindOptions.limit(int)` and
`FindOptions.skip(int)` for these cases.
An example of these methods in action would look like this:

```java
datastore.find(Person.class)
    .iterator(new FindOptions()
	    .skip(1)
	    .limit(10))
```

This query will skip the first element and take up to the next 10 items found by the query.
There's a caveat to using skip/limit for pagination, however.
See the [skip](https://docs.mongodb.com/manual/reference/method/cursor.skip) documentation for more detail.

### Ordering

Ordering the results of a query is done via
[FindOptions.sort(Sort...)](/javadoc/dev/morphia/query/FindOptions.html#sort(dev.morphia.query.Sort...)), etc.
For example, to sort by `age` (youngest to oldest) and then `income` (highest to lowest), you would use this:

```java
getDs().find(User.class)
       .iterator(new FindOptions()
                    .sort(ascending("age"), descending("income"))
                    .limit(1))
       .tryNext();
```

### Tailable Cursors

If you have a [capped collection](https://docs.mongodb.com/manual/core/capped-collections/) it's possible to "tail" a query so that when new documents are added to the collection that match your query, they'll be returned by the
[tailable cursor](https://docs.mongodb.com/manual/reference/glossary/#term-tailable-cursor).
An example of this feature in action can be found in the
[unit tests](https://github.com/MorphiaOrg/morphia/blob/master/morphia/src/test/java/dev/morphia/TestQuery.java) in the `testTailableCursors()` test:

```java
datastore.getMapper().map(CappedPic.class);
getDs().ensureCaps();                                                          // <1>
final Query<CappedPic> query = getDs().find(CappedPic.class);
final List<CappedPic> found = new ArrayList<>();

final MorphiaCursor<CappedPic> tail =
    query.iterator(new FindOptions()
        .cursorType(CursorType.Tailable));
while(found.size() < 10) {
	found.add(tail.next());                                                    // <2>
}
```

There are two things to note about this code sample:

1. This tells Morphia to make sure that any entity configured to use a capped collection has its collection created correctly.
If the collection already exists and is not capped, you will have to manually
[update](https://docs.mongodb.com/manual/core/capped-collections/#convert-a-collection-to-capped) your collection to be a capped collection.
2. Since this `Iterator` is backed by a tailable cursor, `hasNext()` and `next()` will block until a new item is found.
In this version of the unit test, we tail the cursor waiting to pull out objects until we have 10 of them and then proceed with the rest of the application.

### Polymorphic Queries

By default, Morphia will only return documents of the type given in `find()`. However, if you want to retrieve subtypes as well, you can
enable this in `MorphiaConfig` via
[MorphiaConfig#enablePolymorphicQueries()](/javadoc/dev/morphia/config/MorphiaConfig.html#enablePolymorphicQueries()).

{{% notice note %}}
Queries by ID will ignore this setting since such queries are filtering by a specific, unique value and polymorphic queries do incur
potential performance hits due to the wider scope of the query filter.
{{% /notice %}}

### Supported Operators
Every effort is made to provide 100% coverage of all the operators offered by MongoDB. Below is listed all the currently supported
operators. To see an example of an operator in action, click through to see the test cases for that operator.

If an operator is missing and you think it should be included, please file an [issue](https://github.com/MorphiaOrg/morphia/issues) for that operator.

#### Filters

| Operator | Docs | Test Examples |
|----------|------|---------------|
| [$all](http://docs.mongodb.org/manual/reference/operator/query/all) | [Filters#all(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#all(java.lang.String,java.lang.Object)) | [TestAll](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestAll.java) |
| [$and](http://docs.mongodb.org/manual/reference/operator/query/and) | [Filters#and(Filter...)](/javadoc/dev/morphia/query/filters/Filters.html#and(dev.morphia.query.filters.Filter...)) | [TestAnd](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestAnd.java) |
| [$bitsAllClear](http://docs.mongodb.org/manual/reference/operator/query/bitsAllClear) | [Filters#bitsAllClear(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#bitsAllClear(java.lang.String,java.lang.Object)) | [TestBitsAllClear](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestBitsAllClear.java) |
| [$bitsAllSet](http://docs.mongodb.org/manual/reference/operator/query/bitsAllSet) | [Filters#bitsAllSet(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#bitsAllSet(java.lang.String,java.lang.Object)) | [TestBitsAllSet](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestBitsAllSet.java) |
| [$bitsAnyClear](http://docs.mongodb.org/manual/reference/operator/query/bitsAnyClear) | [Filters#bitsAnyClear(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#bitsAnyClear(java.lang.String,java.lang.Object)) | [TestBitsAnyClear](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestBitsAnyClear.java) |
| [$bitsAnySet](http://docs.mongodb.org/manual/reference/operator/query/bitsAnySet) | [Filters#bitsAnySet(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#bitsAnySet(java.lang.String,java.lang.Object)) | [TestBitsAnySet](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestBitsAnySet.java) |
| [$box](http://docs.mongodb.org/manual/reference/operator/query/box) | [Filters#box(String,Point,Point)](/javadoc/dev/morphia/query/filters/Filters.html#box(java.lang.String,com.mongodb.client.model.geojson.Point,com.mongodb.client.model.geojson.Point)) | [TestBox](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestBox.java) |
| [$center](http://docs.mongodb.org/manual/reference/operator/query/center) | [Filters#center(String,Point,double)](/javadoc/dev/morphia/query/filters/Filters.html#center(java.lang.String,com.mongodb.client.model.geojson.Point,double)) | [TestCenter](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestCenter.java) |
| [$centerSphere](http://docs.mongodb.org/manual/reference/operator/query/centerSphere) | [Filters#centerSphere(String,Point,double)](/javadoc/dev/morphia/query/filters/Filters.html#centerSphere(java.lang.String,com.mongodb.client.model.geojson.Point,double)) | [TestCenterSphere](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestCenterSphere.java) |
| [$comment](http://docs.mongodb.org/manual/reference/operator/query/comment) | [Filters#comment(String)](/javadoc/dev/morphia/query/filters/Filters.html#comment(java.lang.String)) | [TestComment](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestComment.java) |
| [$elemMatch](http://docs.mongodb.org/manual/reference/operator/query/elemMatch) | [Filters#elemMatch(Filter...)](/javadoc/dev/morphia/query/filters/Filters.html#elemMatch(dev.morphia.query.filters.Filter...))<br>[Filters#elemMatch(String,Filter...)](/javadoc/dev/morphia/query/filters/Filters.html#elemMatch(java.lang.String,dev.morphia.query.filters.Filter...)) | [TestElemMatch](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestElemMatch.java) |
| [$eq](http://docs.mongodb.org/manual/reference/operator/query/eq) | [Filters#eq(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#eq(java.lang.String,java.lang.Object)) | [TestEq](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestEq.java) |
| [$exists](http://docs.mongodb.org/manual/reference/operator/query/exists) | [Filters#exists(String)](/javadoc/dev/morphia/query/filters/Filters.html#exists(java.lang.String)) | [TestExists](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestExists.java) |
| [$expr](http://docs.mongodb.org/manual/reference/operator/query/expr) | [Filters#expr(Expression)](/javadoc/dev/morphia/query/filters/Filters.html#expr(dev.morphia.aggregation.expressions.impls.Expression)) | [TestExpr](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestExpr.java) |
| [$geoIntersects](http://docs.mongodb.org/manual/reference/operator/query/geoIntersects) | [Filters#geoIntersects(String,Geometry)](/javadoc/dev/morphia/query/filters/Filters.html#geoIntersects(java.lang.String,com.mongodb.client.model.geojson.Geometry)) | [TestGeoIntersects](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestGeoIntersects.java) |
| [$geoWithin](http://docs.mongodb.org/manual/reference/operator/query/geoWithin) | [Filters#geoWithin(String,Polygon)](/javadoc/dev/morphia/query/filters/Filters.html#geoWithin(java.lang.String,com.mongodb.client.model.geojson.Polygon))<br>[Filters#geoWithin(String,MultiPolygon)](/javadoc/dev/morphia/query/filters/Filters.html#geoWithin(java.lang.String,com.mongodb.client.model.geojson.MultiPolygon)) | [TestGeoWithin](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestGeoWithin.java) |
| [$geometry](http://docs.mongodb.org/manual/reference/operator/query/geometry) | [Filters#geometry(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#geometry(java.lang.String,java.lang.Object)) | [TestGeometry](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestGeometry.java) |
| [$gt](http://docs.mongodb.org/manual/reference/operator/query/gt) | [Filters#gt(Object)](/javadoc/dev/morphia/query/filters/Filters.html#gt(java.lang.Object))<br>[Filters#gt(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#gt(java.lang.String,java.lang.Object)) | [TestGt](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestGt.java) |
| [$gte](http://docs.mongodb.org/manual/reference/operator/query/gte) | [Filters#gte(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#gte(java.lang.String,java.lang.Object))<br>[Filters#gte(Object)](/javadoc/dev/morphia/query/filters/Filters.html#gte(java.lang.Object)) | [TestGte](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestGte.java) |
| [$in](http://docs.mongodb.org/manual/reference/operator/query/in) | [Filters#in(String,Iterable)](/javadoc/dev/morphia/query/filters/Filters.html#in(java.lang.String,java.lang.Iterable))<br>[Filters#in(Iterable)](/javadoc/dev/morphia/query/filters/Filters.html#in(java.lang.Iterable)) | [TestIn](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestIn.java) |
| [$jsonSchema](http://docs.mongodb.org/manual/reference/operator/query/jsonSchema) | [Filters#jsonSchema(Document)](/javadoc/dev/morphia/query/filters/Filters.html#jsonSchema(org.bson.Document)) | [TestJsonSchema](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestJsonSchema.java) |
| [$lt](http://docs.mongodb.org/manual/reference/operator/query/lt) | [Filters#lt(Object)](/javadoc/dev/morphia/query/filters/Filters.html#lt(java.lang.Object))<br>[Filters#lt(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#lt(java.lang.String,java.lang.Object)) | [TestLt](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestLt.java) |
| [$lte](http://docs.mongodb.org/manual/reference/operator/query/lte) | [Filters#lte(Object)](/javadoc/dev/morphia/query/filters/Filters.html#lte(java.lang.Object))<br>[Filters#lte(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#lte(java.lang.String,java.lang.Object)) | [TestLte](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestLte.java) |
| [$maxDistance](http://docs.mongodb.org/manual/reference/operator/query/maxDistance) | [Filters#maxDistance(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#maxDistance(java.lang.String,java.lang.Object)) | [TestMaxDistance](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestMaxDistance.java) |
| [$meta](http://docs.mongodb.org/manual/reference/operator/query/meta) | [Meta#indexKey(String)](/javadoc/dev/morphia/query/Meta.html#indexKey(java.lang.String))<br>[Meta#searchHighlights(String)](/javadoc/dev/morphia/query/Meta.html#searchHighlights(java.lang.String))<br>[Meta#searchScore(String)](/javadoc/dev/morphia/query/Meta.html#searchScore(java.lang.String))<br>[Meta#textScore(String)](/javadoc/dev/morphia/query/Meta.html#textScore(java.lang.String)) | [TestMeta](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestMeta.java) |
| [$minDistance](http://docs.mongodb.org/manual/reference/operator/query/minDistance) | [Filters#minDistance(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#minDistance(java.lang.String,java.lang.Object)) | [TestMinDistance](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestMinDistance.java) |
| [$mod](http://docs.mongodb.org/manual/reference/operator/query/mod) | [Filters#mod(String,long,long)](/javadoc/dev/morphia/query/filters/Filters.html#mod(java.lang.String,long,long))<br>[Filters#mod(String,double,double)](/javadoc/dev/morphia/query/filters/Filters.html#mod(java.lang.String,double,double)) | [TestMod](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestMod.java) |
| [$natural](http://docs.mongodb.org/manual/reference/operator/query/natural) | [Sort#naturalAscending()](/javadoc/dev/morphia/query/Sort.html#naturalAscending())<br>[Sort#naturalDescending()](/javadoc/dev/morphia/query/Sort.html#naturalDescending()) | [TestNatural](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestNatural.java) |
| [$ne](http://docs.mongodb.org/manual/reference/operator/query/ne) | [Filters#ne(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#ne(java.lang.String,java.lang.Object)) | [TestNe](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestNe.java) |
| [$near](http://docs.mongodb.org/manual/reference/operator/query/near) | [Filters#near(String,Point)](/javadoc/dev/morphia/query/filters/Filters.html#near(java.lang.String,com.mongodb.client.model.geojson.Point)) | [TestNear](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestNear.java) |
| [$nearSphere](http://docs.mongodb.org/manual/reference/operator/query/nearSphere) | [Filters#nearSphere(String,Point)](/javadoc/dev/morphia/query/filters/Filters.html#nearSphere(java.lang.String,com.mongodb.client.model.geojson.Point)) | [TestNearSphere](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestNearSphere.java) |
| [$nin](http://docs.mongodb.org/manual/reference/operator/query/nin) | [Filters#nin(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#nin(java.lang.String,java.lang.Object)) | [TestNin](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestNin.java) |
| [$nor](http://docs.mongodb.org/manual/reference/operator/query/nor) | [Filters#nor(Filter...)](/javadoc/dev/morphia/query/filters/Filters.html#nor(dev.morphia.query.filters.Filter...)) | [TestNor](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestNor.java) |
| [$not](http://docs.mongodb.org/manual/reference/operator/query/not) | [Filter#not()](/javadoc/dev/morphia/query/filters/Filter.html#not()) | [TestNot](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestNot.java) |
| [$or](http://docs.mongodb.org/manual/reference/operator/query/or) | [Filters#or(Filter...)](/javadoc/dev/morphia/query/filters/Filters.html#or(dev.morphia.query.filters.Filter...)) | [TestOr](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestOr.java) |
| [$polygon](http://docs.mongodb.org/manual/reference/operator/query/polygon) | [Filters#polygon(String,Point...)](/javadoc/dev/morphia/query/filters/Filters.html#polygon(java.lang.String,com.mongodb.client.model.geojson.Point...)) | [TestPolygon](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestPolygon.java) |
| [$regex](http://docs.mongodb.org/manual/reference/operator/query/regex) | [Filters#regex(String,String)](/javadoc/dev/morphia/query/filters/Filters.html#regex(java.lang.String,java.lang.String))<br>[Filters#regex(String,Pattern)](/javadoc/dev/morphia/query/filters/Filters.html#regex(java.lang.String,java.util.regex.Pattern)) | [TestRegex](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestRegex.java) |
| [$size](http://docs.mongodb.org/manual/reference/operator/query/size) | [Filters#size(String,int)](/javadoc/dev/morphia/query/filters/Filters.html#size(java.lang.String,int)) | [TestSize](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestSize.java) |
| [$slice](http://docs.mongodb.org/manual/reference/operator/query/slice) | [ArraySlice#limit(int)](/javadoc/dev/morphia/query/ArraySlice.html#limit(int)) | [TestSlice](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestSlice.java) |
| [$text](http://docs.mongodb.org/manual/reference/operator/query/text) | [Filters#text(String)](/javadoc/dev/morphia/query/filters/Filters.html#text(java.lang.String)) | [TestText](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestText.java) |
| [$type](http://docs.mongodb.org/manual/reference/operator/query/type) | [Filters#type(String,Type...)](/javadoc/dev/morphia/query/filters/Filters.html#type(java.lang.String,dev.morphia.query.Type...)) | [TestType](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestType.java) |
| [$uniqueDocs](http://docs.mongodb.org/manual/reference/operator/query/uniqueDocs) | [Filters#uniqueDocs(String,Object)](/javadoc/dev/morphia/query/filters/Filters.html#uniqueDocs(java.lang.String,java.lang.Object)) | [TestUniqueDocs](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestUniqueDocs.java) |
| [$where](http://docs.mongodb.org/manual/reference/operator/query/where) | [Filters#where(String)](/javadoc/dev/morphia/query/filters/Filters.html#where(java.lang.String)) | [TestWhere](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/filters/TestWhere.java) |
