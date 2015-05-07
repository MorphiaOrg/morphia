+++
date = "2015-03-17T15:36:56Z"
title = "Annotations"
[menu.main]
  parent = "Getting Started"
  identifier = "Annotations"
  weight = 300
  pre = "<i class='fa'></i>"
+++

# Annotations

Below is a list of all the annotations and a brief descriptions of how to use them.

## Entity
Marks entities to be stored directly in a collection. This annotations is optional in most cases (though this is likely to change in 
future versions). There is no harm in including it to be more verbose, and make clear the intention for the class.  The definition of 
this annotation looks like this:

```java
public @interface Entity {
  String value() default Mapper.IGNORED_FIELDNAME;
  CappedAt cap() default @CappedAt(0);
  boolean noClassnameStored() default false;
  boolean queryNonPrimary() default false;
  String concern() default "";
}
```
| Parameter           | Usage           |
| ------------------- | --------------  |
| value()             | Defines the collection to use.  Defaults to using the classname |
| cap()               | Marks this collection as capped and sets the size to use.  See the [`@Capped`]({{< ref "#capped" >}}) below|
| noClassnameStored() | Tells morphia to not store the classname in the document.  The default is to store the classname. |
| queryNonPrimary()   | Indicates that queries against this collection can use secondaries.  The default is primary only reads. |
| concern()           | The WriteConcern to use when writing to this collection. |


## Indexes

In addition to being able to declare an index on a single field you can also declare the indexes at the class level. This allows you to 
 create more than just a single field index; it allows you to create compound indexes with multiple fields.

```java
public @interface Indexes {
    Index[] value();
}
```

### Index
 
```java
public @interface Index {
    String value() default "";
    String name() default "";
    boolean unique() default false;
    boolean dropDups() default false;
    boolean background() default false;
    boolean sparse() default false;
    boolean disableValidation() default false;
    int expireAfterSeconds() default -1;

    Field[] fields() default {};
    IndexOptions options() default @IndexOptions();
}
```

There are two pieces to this annotation that are mutually exclusive.  The first group of parameters are considered legacy.  They are safe
 to use since but are unlikely to survive past the 1.x series.  These options and more have been conglomerated in the `@Field` annotation.
 
| Parameter           | Usage           |
| ------------------- | --------------  |
| value() | List of fields (prepended with "-" for desc; defaults to asc). |  
| name() | The name of the index |
| unique() | Requires values in the index to be unique |
| dropDups() | Drop any duplicate values during the creation of a unique index. |
| background()| Create this index in the background.  There are some [considerations](http://docs.mongodb.org/manual/tutorial/build-indexes-in-the-background/#considerations) to keep in mind.
| sparse() | Create a [sparse](http://docs.mongodb.org/manual/core/index-sparse/) index |
| disableValidation() | By default, morphia will validate field names being index.  This disables those checks. |
| expireAfterSeconds() | Creates a [TTL Index](http://docs.mongodb.org/manual/core/index-ttl/) on a date field. |
| fields() | This is the new way to define which fields to index. [Details]({{< ref "#Field" >}}) can be found below. |
| options() | This is the new way to define index options. [Details]({{< ref "#IndexOptions" >}}) can be found below.  |

#### Field
```java
 public @interface Field {
     String value();
     IndexType type() default IndexType.ASC;
     int weight() default -1;
 }
```

| Parameter           | Usage           |
| ------------------- | --------------  |
| value() | The field to include in the index |
| type() | The type of index to create.  This includes the following values:   `ASC`, `DESC`, `GEO2D`, `GEO2DSPHERE`, `TEXT` |
| weight() | When defining a text index, this is the weight to apply |

#### IndexOptions
```java
public @interface IndexOptions {
    String name() default "";
    boolean unique() default false;
    boolean dropDups() default false;
    boolean background() default false;
    boolean sparse() default false;
    boolean disableValidation() default false;
    int expireAfterSeconds() default -1;
    String language() default "";
    String languageOverride() default "";
}
```
| Parameter           | Usage           |
| ------------------- | --------------  |
| name() | The name of the index |
| unique() | Requires values in the index to be unique |
| dropDups() | Drop any duplicate values during the creation of a unique index. |
| background() | Create this index in the background.  There are some [considerations](http://docs.mongodb.org/manual/tutorial/build-indexes-in-the-background/#considerations) to keep in mind.
| sparse() | Create a [sparse](http://docs.mongodb.org/manual/core/index-sparse/) index |
| disableValidation() | By default, morphia will validate field names being index.  This disables those checks. |
| expireAfterSeconds() | Creates a [TTL Index](http://docs.mongodb.org/manual/core/index-ttl/) on a date field. |
| language() | Default language for the index. |
| languageOverride() | The field in the document to use to override the default language. |

## Id
Marks a field in an `@Entity` to be the "_id" field in mongodb.

## Property
See [PropertyAnnotation](PropertyAnnotation)

## Transient
The field will not be serialized in to mongodb.

## Serialized
The field will be converted to binary and persisted.

## NotSaved
The field will not be saved but can be loaded. Good for data migration.

## AlsoLoad
The field will can be loaded as any of the supplied names. Good for data migration.

## Indexed
The field will be indexed. See [the datastore docs](Datastore).

## Version
Marks a field in an `@Entity` to control optimistic locking for that entity. If the versions change while modifying an entity a
`ConcurrentModificationException` will be throw in the write method (save/delete/etc). This field will be automatically managed for you
-- there is no need to set a value and you should not do so anyway.

```java
@Entity
class MyClass {
   ...
   @Version Long v;
}
```

## Reference
Marks fields as stored in another collection and which are linked (by a dbref reference field). When the Entity is loaded,
the referenced Entity is also loaded.

Attribute name: **lazy**

Instead of loading the referenced field with the Entity, it will be lazily loaded on the first method call of the proxy instance.

Attribute name: **ignoreMissing**

When loading bad references won't generate an exception

Attribute name: **concreteClass**

The class type to create for these references

Look [here](ReferenceAnnotation) for more examples.

## Embedded
Allows customization of certain options. Look [here](EmbeddedAnnotation) for more examples.

## Lifecycle Annotations
See the this [page](LifecycleMethods).
