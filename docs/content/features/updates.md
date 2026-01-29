---
title: "Updates"
weight: 7
description: "Learn how to update documents in Morphia using server-side operations without fetching data across the wire"
---

## Updates

Updates in 2.0, are issued using a `Query` instance . These update operations are executed on the server without fetching any documents across the wire.
Update operations are defined using a set of functions as defined on
[UpdateOperators](/javadoc/dev/morphia/query/updates/UpdateOperators.html).
In our examples, we'll be using the following model:

```java
@Entity("hotels")
public class Hotel
{
   @Id
   private ObjectId id;

   private String name;
   private int stars;

   @Embedded
   private Address address;

   List<Integer> roomNumbers = new ArrayList<Integer>();

   // ... getters and setters
}

@Embedded
public class Address
{
   private String street;
   private String city;
   private String postalCode;
   private String country;

   // ... getters and setters
}
```

### set()/unset()

To change the name of the hotel, one would use something like this:

```java
datastore
    .find(Hotel.class)
    .update(UpdateOperators.set("name", "Fairmont Chateau Laurier"))
    .execute();
```

The `execute()` can optionally take [UpdateOptions](/javadoc/dev/morphia/UpdateOptions.html) if there are any options you might want to apply to your update statement.

Embedded documents are updated the same way.
To change the name of the city in the address, one would use something like this:

```java
datastore
    .find(Hotel.class)
    .update(UpdateOperators.set("address.city", "Ottawa"))
    execute();
```

Values can also be removed from documents as shown below:

```java
datastore
    .find(Hotel.class)
    .update(UpdateOperators.unset("name"))
    execute();
```

After this update, the name of the hotel would be `null` when the entity is loaded.

### Multiple Updates

By default, an update operation will only update the first document matching the query.
This behavior can be modified via the optional
[UpdateOptions](/javadoc/dev/morphia/UpdateOptions.html) parameter on `execute()`:

```java
datastore
    .find(Hotel.class)
    .inc("stars")
    .execute(new UpdateOptions()
        .multi(true));
```

### Upserts

In some cases, updates are issued against a query that might not match any documents.
In these cases, it's often fine for those updates to simply pass with no effect.
In other cases, it's desirable to create an initial document matching the query parameters.
Examples of this might include user high scores, e.g. In cases like this, we have the option to use an upsert:

```java
datastore
    .find(Hotel.class)
    .filter(gt("stars", 100))
    .update()
    .execute(new UpdateOptions()
                     .upsert(true));

// creates { "_id" : ObjectId("4c60629d2f1200000000161d"), "stars" : 50 }
```

### Checking results

In all this one thing we haven't really looked at is how to verify the results of an update.
The `execute()` method returns an instance of
`com.mongodb.client.result.UpdateResult`.
Using this class, you can get specific numbers from the update operation as well as any generated ID as the result of an upsert.

### Returning the updated entity

There are times when a document needs to be updated and also fetched from the database.
In the server documentation, this is referred to as [findAndModify](https://docs.mongodb.com/manual/reference/method/db.collection.findAndModify/).
In Morphia, this functionality is exposed through the
[Query#modify()](/javadoc/dev/morphia/query/Query.html#modify(dev.morphia.query.updates.UpdateOperator,dev.morphia.query.updates.UpdateOperator...))
method. With this method, you can choose to return the updated entity in either the
state before or after the update. The default is to return the entity in the _before_ state.
This can be changed by passing in a `ModifyOptions` reference to the operation:

```java
datastore
    .find(Hotel.class)
    .modify(UpdateOperators.set("address.city", "Ottawa"))
    execute(new ModifyOptions()
        .returnDocument(ReturnDocument.AFTER));
```

### Merges

A specialized form of an update is the [merge()](/javadoc/dev/morphia/Datastore.html#merge(T)) operation.
A common case in scenarios where multiple subsystems or applications share a database is where one part of the system might only have part of a larger entity. e.g., a
[DTO](https://en.wikipedia.org/wiki/Data_transfer_object) might only have a subset of an entity necessary to perform the duties of that particular service.
Updating the database with the partial entities can be problematic because it forces the application developer to inspect an entity's state and manually generate the correct set of update statements.
Morphia 2.0 introduced the `merge()` method to help cover this case.
Using `merge()`, a specific set of `$set` updates are issued to only save fields as defined on the DTO type itself (which, of course, must be annotated with `@Entity` like any other Morphia type).

This is useful but limited.
What happens to any null fields and empty Lists?
Using `MorphiaConfig`, Morphia can be configured to not persist those fields such that they never end up in the database.
In the above scenario, however, Morphia is only issuing a `$set` for the fields that should be persisted which leaves potentially outdated information in the database. (e.g., say a process removes the final item from a List in memory.  `merge()` would actually *leave* that last item in the database because an update would not be issued for that empty List.) In 2.2, a new value is added to the optional [InsertOneOptions](/javadoc/dev/morphia/InsertOneOptions.html) to account for this.

Setting [unsetMissing](/javadoc/dev/morphia/InsertOneOptions.html#unsetMissing(boolean)) to true, any property defined on an entity
that isn't getting updated via `$set` will have a `$unset` operator defined.
This will result in null properties and empty Lists getting removed from documents in the database so that they will reflect the current state in memory.

Regardless of whether this value is set, `merge()` will issue a `find()` for that entity and will return the updated form from the database.
Without using `unsetMissing()`, this is useful for merging the in memory state with what's in the database.
With this value set, the two should be identical, of course.

### Supported Operators
Every effort is made to provide 100% coverage of all the operators offered by MongoDB. Below is listed all the currently supported
operators. To see an example of an operator in action, click through to see the test cases for that operator.

If an operator is missing and you think it should be included, please file an [issue](https://github.com/MorphiaOrg/morphia/issues) for that operator.

#### Filters

| Operator | Docs | Test Examples |
|----------|------|---------------|
| [$addToSet](http://docs.mongodb.org/manual/reference/operator/query/addToSet) | [UpdateOperators#addToSet(String,Object)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#addToSet(java.lang.String,java.lang.Object))<br>[UpdateOperators#addToSet(String,List)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#addToSet(java.lang.String,java.util.List)) | [TestAddToSet](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestAddToSet.java) |
| [$bit](http://docs.mongodb.org/manual/reference/operator/query/bit) | [UpdateOperators#and(String,int)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#and(java.lang.String,int))<br>[UpdateOperators#bit(String,Number)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#bit(java.lang.String,java.lang.Number))<br>[UpdateOperators#or(String,int)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#or(java.lang.String,int))<br>[UpdateOperators#xor(String,int)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#xor(java.lang.String,int)) | [TestBit](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestBit.java) |
| [$currentDate](http://docs.mongodb.org/manual/reference/operator/query/currentDate) | [UpdateOperators#currentDate(String)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#currentDate(java.lang.String)) | [TestCurrentDate](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestCurrentDate.java) |
| [$dec](http://docs.mongodb.org/manual/reference/operator/query/dec) | [UpdateOperators#dec(String)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#dec(java.lang.String))<br>[UpdateOperators#dec(String,Number)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#dec(java.lang.String,java.lang.Number)) | [TestDec](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestDec.java) |
| [$each](http://docs.mongodb.org/manual/reference/operator/query/each) | [UpdateOperators#push(String,List)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#push(java.lang.String,java.util.List)) | [TestEach](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestEach.java) |
| [$inc](http://docs.mongodb.org/manual/reference/operator/query/inc) | [UpdateOperators#inc(String,Number)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#inc(java.lang.String,java.lang.Number))<br>[UpdateOperators#inc(String)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#inc(java.lang.String)) | [TestInc](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestInc.java) |
| [$max](http://docs.mongodb.org/manual/reference/operator/query/max) | [UpdateOperators#max(String,Number)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#max(java.lang.String,java.lang.Number))<br>[UpdateOperators#max(String,Temporal)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#max(java.lang.String,java.time.temporal.Temporal))<br>[UpdateOperators#max(String,Date)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#max(java.lang.String,java.util.Date)) | [TestMax](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestMax.java) |
| [$min](http://docs.mongodb.org/manual/reference/operator/query/min) | [UpdateOperators#min(String,Number)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#min(java.lang.String,java.lang.Number))<br>[UpdateOperators#min(String,Temporal)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#min(java.lang.String,java.time.temporal.Temporal))<br>[UpdateOperators#min(String,Date)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#min(java.lang.String,java.util.Date)) | [TestMin](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestMin.java) |
| [$mul](http://docs.mongodb.org/manual/reference/operator/query/mul) | [UpdateOperators#mul(String,Number)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#mul(java.lang.String,java.lang.Number)) | [TestMul](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestMul.java) |
| [$pop](http://docs.mongodb.org/manual/reference/operator/query/pop) | [UpdateOperators#pop(String)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#pop(java.lang.String)) | [TestPop](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestPop.java) |
| [$pull](http://docs.mongodb.org/manual/reference/operator/query/pull) | [UpdateOperators#pull(String,Filter...)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#pull(java.lang.String,dev.morphia.query.filters.Filter...))<br>[UpdateOperators#pull(String,Object)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#pull(java.lang.String,java.lang.Object)) | [TestPull](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestPull.java) |
| [$pullAll](http://docs.mongodb.org/manual/reference/operator/query/pullAll) | [UpdateOperators#pullAll(String,List)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#pullAll(java.lang.String,java.util.List)) | [TestPullAll](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestPullAll.java) |
| [$push](http://docs.mongodb.org/manual/reference/operator/query/push) | [UpdateOperators#push(String,Object)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#push(java.lang.String,java.lang.Object)) | [TestPush](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestPush.java) |
| [$rename](http://docs.mongodb.org/manual/reference/operator/query/rename) | [UpdateOperators#rename(String,String)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#rename(java.lang.String,java.lang.String)) | [TestRename](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestRename.java) |
| [$set](http://docs.mongodb.org/manual/reference/operator/query/set) | [UpdateOperators#set(String,Object)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#set(java.lang.String,java.lang.Object))<br>[UpdateOperators#set(Object)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#set(java.lang.Object)) | [TestSet](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestSet.java) |
| [$setOnInsert](http://docs.mongodb.org/manual/reference/operator/query/setOnInsert) | [UpdateOperators#setOnInsert(Map)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#setOnInsert(java.util.Map)) | [TestSetOnInsert](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestSetOnInsert.java) |
| [$unset](http://docs.mongodb.org/manual/reference/operator/query/unset) | [UpdateOperators#unset(String,String...)](/javadoc/dev/morphia/query/updates/UpdateOperators.html#unset(java.lang.String,java.lang.String...)) | [TestUnset](https://github.com/MorphiaOrg/morphia/blob/master/core/src/test/java/dev/morphia/test/query/updates/TestUnset.java) |
