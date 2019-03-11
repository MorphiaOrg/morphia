+++
title = "References"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

Morphia supports two styles of defining references:  the `@Reference` annotation and the experimental [`MorphiaReference`]({{< apiref 
"dev/morphia/mapping/experimental/MorphiaReference" >}}).  The annotation based approach is discussed 
[here]({{< ref "guides/annotations" >}}).  This guide will cover the wrapper based approach.

{{% note class="important" %}}
This API is experimental.  Its implementation and API subject to change based on user feedback.  It's ultimate inclusion in a public 
permanent API can not be guaranteed.  That said, users are encouraged to experiment with the API and provide as much feedback as possible
 both positive and negative.
{{% /note %}}

An alternative to the traditional annotation-based approach is the [`MorphiaReference`]({{< apiref
 "dev/morphia/mapping/experimental/MorphiaReference" >}}) wrapper.  This type can not be instantiated directly.  Instead a `wrap()` 
 method is provided that will construct the proper type and track the necessary state.  Currently, four different types of values are 
 supported by `wrap()`:  a reference to a single entity, a List of references to entities, a Set, and a Map.  `wrap()` will determine how
  best to handle the type passed and create the appropriate structures internally.  This is how this type might be used in practice:
  
```java
private class Author {
    @Id
    private ObjectId id;
    private String name;
    private MorphiaReference<List<Book>> list;
    private MorphiaReference<Set<Book>> set;
    private MorphiaReference<Map<String, Book>> map;

    public Author() { }

    public List<Book> getList() {
        return list.get();
    }

    public void setList(final List<Book> list) {
        this.list = MorphiaReference.wrap(list);
    }

    public void setList(final String collection, final List<Book> list) {
        this.list = MorphiaReference.wrap(collection, list);
    }

    public Set<Book> getSet() {
        return set.get();
    }

    public void setSet(final Set<Book> set) {
        this.set = MorphiaReference.wrap(set);
    }

    public void setSet(final String collection, final Set<Book> set) {
        this.set = MorphiaReference.wrap(collection, set);
    }

    public Map<String, Book> getMap() {
        return map.get();
    }

    public void setMap(final Map<String, Book> map) {
        this.map = MorphiaReference.wrap(map);
    }

    public void setMap(final String collection,  final Map<String, Book> map) {
        this.map = MorphiaReference.wrap(collection, map);
    }
}

private class Book {
    @Id
    private ObjectId id;
    private String name;
    private MorphiaReference<Author> author;

    public Book() { }

    public Author getAuthor() {
        return author.get();
    }

    public void setAuthor(final Author author) {
        this.author = MorphiaReference.wrap(author);
    }

    public void setAuthor(final String collection, final Author author) {
        this.author = MorphiaReference.wrap(collection, author);
    }
}

```

As you can see we have 3 different references from `Author` to `Book` and one in the opposite direction.  It would also be good to note 
that the public API of those two classes don't expose the `MorphiaReference` externally.  This is, of course, a stylistic choice but is 
the encouraged approach as it avoids leaking out implementation and mapping details outside of your model.  Looking at `Author#setList()
`, you'll note we have two variants.  These two classes are extracted from [MorphiaReferenceTest.java]({{< srcref
"morphia/src/test/java/dev/morphia/mapping/experimental/MorphiaReferenceTest.java">}}) test.  In practice, you're unlikely to need both 
versions of `setList()` but in this case they help to illustrate, and test, a feature.

The basic of `setList()` accepts a `List<Book>` and stores them as references to `Book` instances stored in the collection as defined by 
the mapping metadata for `Book`.  In this particular case, `Book` does not have an explicit collection named and so the class name of 
`Book` is used to determine the collection name.  Because these references point to the mapped collection name for the type, we can get 
away with storing only the `_id` fields for each book.  This gives us data in the database that looks like this:

```javascript
> db.Author.find().pretty()
{
	"_id" : ObjectId("5c3e99276a44c77dfc1b5dbd"),
	"className" : "dev.morphia.mapping.experimental.MorphiaReferenceTest$Author",
	"name" : "Jane Austen",
	"list" : [
		ObjectId("5c3e99276a44c77dfc1b5dbe"),
		ObjectId("5c3e99276a44c77dfc1b5dbf"),
		ObjectId("5c3e99276a44c77dfc1b5dc0"),
		ObjectId("5c3e99276a44c77dfc1b5dc1"),
		ObjectId("5c3e99276a44c77dfc1b5dc2")
	]
}
```

As you can see, we only need to store the ID values because the collection is already known elsewhere.  However, sometimes we need to 
refer to documents stored in different collections.  For these cases, there is a `wrap(String collection, final V value)` overload.  This
 instructs the `MorphiaReference` wrapper to use a different collection.  Using this version, we get data in the database that looks like
  this:
  
```javascript
> db.jane.find().pretty()
{
	"_id" : ObjectId("5c3e99c06a44c77e5a9b5701"),
	"className" : "dev.morphia.mapping.experimental.MorphiaReferenceTest$Author",
	"name" : "Jane Austen",
	"list" : [
		DBRef("books", ObjectId("5c3e99c06a44c77e5a9b5702")),
		DBRef("books", ObjectId("5c3e99c16a44c77e5a9b5703")),
		DBRef("books", ObjectId("5c3e99c16a44c77e5a9b5704")),
		DBRef("books", ObjectId("5c3e99c16a44c77e5a9b5705")),
		DBRef("books", ObjectId("5c3e99c16a44c77e5a9b5706"))
	]
}
``` 

In both cases, we have a document field called `list` but as you can see in the second case, we're not storing just the `_id` values but 
`DBRef` instances storing both the collection name, "books" in this case, and `ObjectId` values from the Books.  This lets the wrapper 
properly reconstitute these references when you're ready to use them.

{{% note %}}
Before we go too much further, it's important to point that, regardless of the type of the references, they are fetched lazily.  So if 
you multiple fields with referenced entities, they will not be fetched until you call `get()` on the `MorphiaReference`.  If the type is 
a `Collection` or a `Map`, all the referenced entities are fetched and loaded via a single query.  This saves on server round trips but 
does raise the risk of potential `OutOfMemoryError` problems.
{{% /note %}}

A `Set` of references will look no different in the database than the `List` does.  However, `Map`s of references are slightly more 
complicated.  With the default, mapped collections being used, a `Map` would look something like this:

```javascript
> db.Author.find().pretty()
{
	"_id" : ObjectId("5c3e9cad6a44c77fa8f38f58"),
	"className" : "dev.morphia.mapping.experimental.MorphiaReferenceTest$Author",
	"name" : "Jane Austen",
	"map" : {
		"Sense and Sensibility " : ObjectId("5c3e9cad6a44c77fa8f38f59"),
		"Pride and Prejudice" : ObjectId("5c3e9cad6a44c77fa8f38f5a"),
		"Mansfield Park" : ObjectId("5c3e9cad6a44c77fa8f38f5b"),
		"Emma" : ObjectId("5c3e9cad6a44c77fa8f38f5c"),
		"Northanger Abbey" : ObjectId("5c3e9cad6a44c77fa8f38f5d")
	}
}
``` 

Using custom collections, the `Map`s change slightly:

```javascript
> db.jane.find().pretty()
{
	"_id" : ObjectId("5c3e9d096a44c77fd500e03f"),
	"className" : "dev.morphia.mapping.experimental.MorphiaReferenceTest$Author",
	"name" : "Jane Austen",
	"map" : {
		"Sense and Sensibility " : DBRef("books", ObjectId("5c3e9d096a44c77fd500e040")),
		"Pride and Prejudice" : DBRef("books", ObjectId("5c3e9d0a6a44c77fd500e041")),
		"Mansfield Park" : DBRef("books", ObjectId("5c3e9d0a6a44c77fd500e042")),
		"Emma" : DBRef("books", ObjectId("5c3e9d0a6a44c77fd500e043")),
		"Northanger Abbey" : DBRef("books", ObjectId("5c3e9d0a6a44c77fd500e044"))
	}
}
```

References to single entities will follow the same pattern with regards to the `_id` values vs `DBRef` entries.

{{% note  %}}
Currently there is no support for configuring the `ignoreMissing` parameter as there is via the annotation.  The wrapper will silently drop 
missing ID values or return null depending on the type of the reference.  Depending on the response to this feature in general 
consideration can be given to adding such functionality in the future.
{{% /note %}}
