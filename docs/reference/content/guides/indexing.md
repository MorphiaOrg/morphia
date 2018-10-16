+++
title = "Indexing"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

Morphia provides annotations that allow developers to define indexes for a collection to be defined alongside the other mapping data on
an entity's source.  In addition to the familiar ascending/descending index types, Morphia and MongoDB support [TTL]({{< docsref
"core/index-ttl/" >}}), [text]({{< docsref "core/index-text/" >}}), and [geospatial]({{< docsref "applications/geospatial-indexes/" >}})
 indexes.   When defining [text]({{< relref "#text-indexing" >}}) indexes there are certain restrictions which will be covered below.  Full
  details for all these types are available in the [manual]({{< docsref "indexes" >}}).

There are two ways to define indexes:  at the class level and at the field level.

## Class Level Indexes

Class level indexing begins with the [__@Indexes__]({{< ref "/guides/annotations.md#indexes" >}}) annotation.  This is a container
annotation whose sole purpose is to hold a number of [__@Index__]({{< ref "/guides/annotations.md#index" >}}) annotations.  This annotation
has two primary components to cover here:  __fields__ and __options__.  An index definition would take the following form:

```java
@Entity
@Indexes({
    @Index(fields = @Field("field2", type = DESC)),
    @Index(fields = @Field("field3", options = @IndexOptions(name = "indexing_test")))
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

The fields to use in an index definition are defined with the [__@Field__]({{< ref "/guides/annotations.md#field" >}}) annotation. An
arbitrary number of __@Field__s can be given but at least one must be present.

#### value()
Indicates which field to use for indexing.  The name used for the field can be either the Java field name or the mapped document field
name as defined in the class's mapping via, e.g.,  the [__@Property__]({{< ref "/guides/annotations.md#property" >}}) or
 [__@Embedded__]({{< ref "/guides/annotations.md#embedded" >}}) annotations.  For most index types, this value is validated by default.  An
  exception is made for [text indexes]({{< relref "#text-indexing" >}}) as discussed below.

#### type()
*Default: IndexType.ASC*

Indicates the "type" of the index (ascending, descending, geo2D, geo2d sphere, or text) to create on the field.

*See [IndexType](/javadoc/xyz/morphia/utils/IndexType.html)*

#### weight()
*Optional*

Specifies the weight to use when creating a text index.  This value only makes sense when direction is __IndexType.TEXT__.

### Index Options

Options for an index are defined on the [__@IndexOptions__]({{< ref "/guides/annotations.md#indexoptions" >}}).  More complete coverage can
be found in the [manual]({{< docsref "reference/method/db.collection.createIndex/#options" >}}) but we'll provide some basic coverage
here as well.

#### background()
*Default: false*

This value determines if the index build is a blocking call or not.  By default, creating an index blocks all other operations on a
database.  When building an index on a collection, the database that holds the collection is unavailable for read or write operations
until the index build completes. For potentially long running index building operations, consider the **background** operation so that the
MongoDB database remains available during the index building operation.  The MongoDB [manual]({{< docsref
"core/index-creation/#background-construction" >}}) has more detail.

#### disableValidation()
*Default: false*

When ensuring indexes in the database, Morphia will attempt to ensure that the field names match either the Java field names or the
mapped document names.  Setting this to __true__ disables this validation.

#### dropDups()
*Default: false*

When defining a [unique]({{< relref "#unique" >}}) index, if there are duplicate values found, the index creation will.  Setting this value to true will instruct MongoDB to drop the documents with duplicate values.

{{% note class="important" %}}
As of MongoDB version 3.0, the dropDups option is no longer available.
{{% /note %}}

#### expireAfterSeconds()
*Optional*

Specifies a value, in seconds, as a [TTL]({{< docsref "core/index-ttl/" >}}) to control how long MongoDB retains documents in
this collection.  The field listed must contain values that are dates.

#### language()
*Optional*

For [text indexes]({{< relref "#text-indexing" >}}), the language that determines the list of stop words and the rules for the stemmer and
tokenizer. See [Text Search Languages]({{< docsref "reference/text-search-languages/" >}}) for the available languages and [Specify a
Language for Text Index]({{< docsref "tutorial/specify-language-for-text-index" >}}) for more information and examples. The default value
 is **english**.

#### languageOverride()
*Optional*

For [text indexes]({{< relref "#text-indexing" >}}), the name of the field in the collection’s documents that contains the
override language for the document. The default value is **language**. See [Use any Field to Specify the Language for a Document]({{<
docsref "tutorial/specify-language-for-text-index/#specify-language-field-text-index-example" >}}) for an example.

#### name()
*Optional*

The name of the index. If unspecified, MongoDB generates an index name by concatenating the names of the indexed fields and
the sort order.

Whether user specified or MongoDB generated, index names including their full namespace (i.e. database.collection) cannot be longer than
 the [Index Name Limit]({{< docsref "reference/limits/#Index-Name-Length" >}}).

#### sparse()
*Default: false*

If __true__, the index only references documents with the specified field. These indexes use less space but behave differently in
some situations (particularly sorts).  See [Sparse Indexes]({{< docsref "core/index-sparse/" >}}) for more
information.

#### unique()
*Default: false*

Creates a unique index so that the collection will not accept insertion of documents where the index key or keys match an
existing value in the index.  Specify __true__ to create a unique index.

#### partialFilter()
*Optional*

New in MongoDB 3.2, [partial indexes](https://docs.mongodb.com/v3.2/core/index-partial/) only index the documents in a collection that meet
 a specified filter expression thereby reducing storage and maintenance costs.  A partial filter is defined using a query as shown here:
 
```java
@Indexes({@Index(options = @IndexOptions(partialFilter = "{ name : { $exists : true } }"),
        fields = {@Field(value = "name")})})
    public static class SomeClass { ... }
```

#### collation()
*Optional*

Collation allows users to specify language-specific rules for string comparison, such as rules for lettercase and accent marks.  A collation
can be defined using the __collation()__ property on __@IndexOptions__ and takes an [__@Collation__]({{< ref "/guides/annotations.md#collation" >}})
instance.

## Field Level Indexes

Field level indexing is a simpler approach to defining a basic, single key index.  These indexes are defined by applying the
[__@Indexed__] ({{< ref "/guides/annotations.md#indexed" >}}) annotation to a particular field on a class.  Because the index definition is
applied at the field level, the index is created using only that field and so the [__@Field__]({{< ref "/guides/annotations.md#field" >}})
annotations are unnecessary.  The options for the index are the same as defined [above]({{< relref "#options" >}}).  A field level index
definition would look like this:

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

## Text Indexing

Morphia's indexing supports MongoDB's text indexing and search functionality as we've briefly seen above.  Full details can be found in
the [manual]({{< docsref "core/index-text/" >}}) but there are a few Morphia specific details to cover.  Indexed field names are validated
by default but validation is disabled when an index is defined using MongoDB's
[__$**__]({{< docsref "core/index-text/#text-index-wildcard" >}}) syntax.  This special instruction tells MongoDB to create a text index on
all fields with string content in a document.  A [compound index]({{< docsref "core/index-text/#compound-index" >}}) can be created
incorporating a text index but it's important to note there can only be one text index on a collection.

A wild card text index declaration would look like this:

```java
    @Indexes(@Index(fields = @Field(value = "$**", type = TEXT)))
```

{{% note class="important" %}}
A collection can have at most one text index.
{{% /note %}}
