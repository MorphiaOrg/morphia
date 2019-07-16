2.0 Migration Notes
===

These are the major goals of the 2.0 release:
1.  Modernization of the implementation
1.  Clean up and modernization of the API
1.  Attempt to make the API more self-consistent.

This release aims to make the API smaller, lighter, and more fluent.  The pre-2.0 deprecations are being removed and new deprecations 
added to further improve the API and the developer experience.  As such there are a few things to note when upgrading.  This document 
will attempt to document these items as development progresses.  Any early adopters finding missing or unclear items please file a bug.

*  Many methods on `Datastore` are being deprecated in favor of more fluent APIs with `Query` as the root entry point.  In most cases, 
much of your code can remain as it is but with a slight rearrangement it can take advantage of the fluent API.  e.g., when performing an 
update operation, this code:

```java
getDs().update(
    getDs().find(SomeEntity.class).field("name").equalIgnoreCase("Robert"),
    getAds().createUpdateOperations(SomeEntity.class)
        .removeAll("nicknames", "Shorty"));
```

becomes this:

```java
getDs().find(SomeEntity.class)
    .field("name").equalIgnoreCase("Robert")
    .update()
        .removeAll("nicknames", "Shorty")
    .execute();
```

* `Query#find()` is being renamed to `execute()` for consistency with the update/delete operations started via a query.
* Some of the old `update()` methods made certain assumptions with `multi` update being the default.  In keeping with the change of the 
server defaulting to single document updates, Morphia defaults to single document updates as well unless explicitly set to update 
multiple documents.  See the [`update()`](https://docs.mongodb.com/manual/reference/method/db.collection.update/) documentation for details.
* Iterable parameters have been changed to List.  Lists are easier to work with and `List.of()` makes creating them from arrays, e.g., 
trivial
* `@Embedded` is now only allowed on the embedded type.  If you wish to map a specific name to a field rather than using the field name, 
use `@Property` on that field.
* Morphia's geojson objects have been deprecated.  Any use via the API will be transparently converted to the driver's native types but 
applications should be updated to use those types directly.  Any use of those types as fields on entities will break as there will be no 
codecs defined for those types.