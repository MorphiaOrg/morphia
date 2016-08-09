+++
date = "2015-03-17T15:36:56Z"
title = "Annotations"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

# Annotations

Below is a list of all the annotations and a brief description of how to use them.

## Indexes

Indexes can be defined on each field directly for single field indexing or at the class level for compund indexes.  To see the next few
annotations in context, please refer to [TestIndexCollections.java]({{< srcref
"morphia/src/test/java/org/mongodb/morphia/TestIndexCollections.java">}}) or [TestIndexed.java]({{< srcref
 "morphia/src/test/java/org/mongodb/morphia/indexes/TestIndexed.java">}}) in the Morphia source.

### Index

The `@Index` documentation can be found [here]({{< apiref "org/mongodb/morphia/annotations/Index" >}}).  There are two pieces to this 
annotation that are mutually exclusive.  The first group of parameters are considered legacy.  They are safe to use but will be removed 
in the 2.x series.  These options and more have been conglomerated in the [`@IndexOptions`]({{< apiref "org/mongodb/morphia/annotations/IndexOptions" >}}) annotation.

#### Field
The [`@Field`]({{< apiref "org/mongodb/morphia/annotations/Field" >}}) annotation defines indexing on a specific document field.  Multiple
instances of this annotation may be passed to the `@Index` annotation to define a compound index on multiple fields.

#### IndexOptions
The [`@IndexOptions`]({{< apiref "org/mongodb/morphia/annotations/IndexOptions" >}}) annotation defines the options to apply to an index
definition.  This annotation replaces the fields found directly on the `@Index` annotation.  This annotation was added to ensure that index
options are consistent across the various index definition approaches.

#### Indexed
[`@Indexed`]({{< apiref "org/mongodb/morphia/annotations/Indexed" >}}), applied to a Java field, marks the field to be indexed by MongoDB.
  This is used for simple, single-field indexes.  As stated above, the `options` value replaces the individual setting values on the
   `@Indexed` annotation itself.

## Entity Mapping
Morphia provides a number of annotations providing for the customization of object mapping.

### Entity
[`@Entity`]({{< apiref "org/mongodb/morphia/annotations/Entity" >}}) marks entities to be stored directly in a collection. This annotation
is optional in most cases but is required if an entity is to be mapped to a specifically named collection.  If no mapping is given, the 
collection is named after the class itself.  There are two different mechanisms for mapping cross-object relationships in Morphia:
references and embedding.

### Reference
[`@Reference`]({{< apiref "org/mongodb/morphia/annotations/Reference" >}}) marks a field as a reference to a document stored in another
collection and is linked (by a `DBRef` field). When the Entity is loaded, the referenced entity is also be loaded.  Any object referenced 
via an `@Reference` field must have already have a non-null `@Id` value in the referenced entity. This can be done by either saving the 
referenced entities first or by manually assigning them ID values.  By default, these referenced entities are automatically loaded by 
Morphia along with the referencing entity.  This can result in a high number of database round trips just to load a single entity.  To 
resolve this, `lazy = true` can be passed to the annotation.  This will create a dynamic proxy which will lazily load the entity the first 
time it is referenced in code.  

Fields annotated with `@Reference` will show up in MongoDB as `DBRef` fields by default.  A `DBRef` stores not only the entity's ID value 
but also the collection name.  In most cases, this is probably redundant information as the collection name is already encoded in the 
entity's mapping information.  To reduce the amount of storage necessary to track these references, use `idOnly = true` in the mapping.
This will result in only the ID value being stored in the document.
 
### Embedded
In contrast to `@Reference` where a nested Java reference ends up as a separate document in a collection, 
[`@Embedded`]({{< apiref "org/mongodb/morphia/annotations/Embedded" >}}) tells Morphia to embed the document created from the Java object
in the document of the parent object.  This annotation can be applied to the class of the embedded type or on the field holding the
embedded instance.

### Id
[`@Id`]({{< apiref "org/mongodb/morphia/annotations/Id" >}}) marks a field in an entity to be the `_id` field in MongoDB.  This 
annotation is required on all top level entities regardless of the presence of an `@Entity` annotation.  If a class is marked with 
`@Embedded` this annotation is not required since embedded documents are not required to have _id fields.

### Property
[`@Property`]({{< apiref "org/mongodb/morphia/annotations/Property" >}}) is an optional annotation instructing Morphia to persist the 
field using the given name in the document saved in MongoDB.  By default, the field name is used as the property name.  This can be
overridden by passing a String with the new name to the annotation.

### Transient
[`@Transient`]({{< apiref "org/mongodb/morphia/annotations/Transient" >}}) instructs Morphia to ignore this field when converting an 
entity to a document.  The Java keyword `transient` can also be used instead.

### Serialized
[`@Serialized`]({{< apiref "org/mongodb/morphia/annotations/Serialized" >}}) instructs Morphia to serialize this field using JDK 
serialization.  The field's value gets converted to a `byte[]` and passed to MongoDB.

### NotSaved
[`@NotSaved`]({{< apiref "org/mongodb/morphia/annotations/NotSaved" >}}) instructs Morphia to ignore this field when saving but will
 still be loaded from the database when the entity is read.

### AlsoLoad
[`@AlsoLoad`]({{< apiref "org/mongodb/morphia/annotations/AlsoLoad" >}}) instructs Morphia to look for a field under different names than 
the mapped name.  When a field gets remapped to a new name, you can either update the database and migrate all the fields at once or use 
this annotation to tell Morphia what older names to try if the current one fails.  It is an error to have values under both the old and 
new key names when loading a document.  These alternate names are not used in queries, however, so if there are queries against this field
they should be updated to use the alternate names as well or the database should be updated such that every instance of the old name is 
renamed.

### Version
[`@Version`]({{< apiref "org/mongodb/morphia/annotations/Version" >}}) marks a field in an entity to control optimistic locking. If the
versions change in the database while modifying an entity (including deletes) a `ConcurrentModificationException` will be thrown. This 
field will be automatically managed for you -- there is no need to set a value and you should not do so.  If another name beside the Java
field name is desired, a name can be passed to this annotation to change the document's field name.

## Lifecycle Annotations

There are various annotations which can be used to register callbacks on certain lifecycle events. These include Pre/Post-Persist, Pre-Save, and Pre/Post-Load.

- `@PreLoad` - Called before mapping the datastore object to the entity (POJO); the DBObject is passed as an argument (you can add/remove/change values)
- `@PostLoad` - Called after mapping to the entity
- `@PrePersist` - Called before save, it can return a DBObject in place of an empty one.
- `@PreSave` - Called before the save call to the datastore
- `@PostPersist` - Called after the save call to the datastore

### Examples
[This](https://github.com/mongodb/morphia/blob/master/morphia/src/test/java/org/mongodb/morphia/TestQuery.java#L63) is one of the test
classes.

All parameters and return values are optional in your implemented methods.

#### `@PrePersist`
Here is a simple example of an entity that always saves the Date it was last updated at.
```java
class BankAccount {
  @Id String id;
  Date lastUpdated = new Date();

  @PrePersist void prePersist() {lastUpdated = new Date();}
}
```

#### `@EntityListeners`
In addition, you can separate the lifecycle event implementation in an external class, or many.
```java
@EntityListeners(BackAccountWatcher.class)
public class BankAccount {
  @Id String id;
  Date lastUpdated = new Date();
}

class BankAccountWatcher{

  @PrePersist void prePersist(BankAccount act) {act.lastUpdated = new Date();}

}
```
