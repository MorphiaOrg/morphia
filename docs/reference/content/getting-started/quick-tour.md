+++
date = "2015-03-17T15:36:56Z"
title = "Quick Tour"
[menu.main]
  parent = "Getting Started"
  identifier = "Quick Tour"
  weight = 2
  pre = "<i class='fa'></i>"
+++

# Quick Tour

Morphia wraps the mongodb Java driver so some level of familiarity with using the driver can be helpful.  Morphia does its best to 
abstract much of that way but if something is confusing, it wouldn't hurt to consult the Java driver [docs](http://mongodb.github
.io/mongo-java-driver/) as well.

The following code snippets come from the `QuickTour.java` example code
that can be found with the [morphia source]({{< srcref "morphia/src/test/examples/java/org/mongodb/morphia/examples/QuickTour.java">}}).

## Setting up Morphia

The following example shows how to create the initial Morphia instance.  Using this instance, you can configure various aspects of 
Morphia maps your entities and validates your queries.


```java
final Morphia morphia = new Morphia();

// tell morphia where to find your classes
// can be called multiple times with different packages or classes
morphia.mapPackage("org.mongodb.morphia.example");

// create the Datastore connecting to the database running on the default port on the local host
final Datastore datastore = morphia.createDatastore(new MongoClient(), "morphia_example");
datastore.ensureIndexes();;
```

This snippet creates the morphia instance we'll be using in our simple application.  The `Morphia` class exists to configure the `Mapper`
 to be used and to define various system-wide defaults.  It is also what is used to create the `Datastore` we'll be using.  With this 
 approach we could conceivable configure morphia once and then connect to multiple databases by creating different `Datastore` instances.
   In practice, this is likely pretty rare but it is possible.  The `Datastore` takes two parameters:  the `MongoClient` used to connect 
   to the database process and the name of the database to use in the `Datastore`.
   
The second line, which we skipped over, deserves a bit of consideration.  In this case, we're tell morphia to look at every class in the 
package we've given and find every class annotated with `@Entity` (which we'll cover shortly) and discover the mapping metadata we've 
put on our classes.  There are several variations of mapping that can be done and they can be called multiple times with different values
 to properly cover all your entities wherever they might live in your application.
 
## Mapping Classes

There are two ways that morphia can handle your classes:  as top level entities or embedded.  Any class annotated with `@Entity` is 
treated as a top level document stored directly in a collection.  Any class with `@Entity` must have a field annotated with `@Id` to 
define which field to use as the `_id` value in the document written to mongodb.  `@Embedded` indicates that the class will result in a 
subdocument inside another document.  `@Embedded` classes do not require the presence of an `@Id` field.

```java
@Entity("employees")
@Indexes(
    @Index(value = "salary", fields = @Field("salary"))
)
class Employee {
    @Id
    private ObjectId id;
    private String name;
    @Reference
    private Employee manager;
    @Reference
    private List<Employee> directReports;
    @Property("wage")
    private Double salary;
}
```

There are a few things here to discuss and others we'll defer to later sections.  This class is annotated using the `@Entity` annotation 
so we know that it will be a top level document.  In the annotation, you'll see `"employees"`.  By default, morphia will use the class 
name as the collection name.  If you pass a String instead, it will use that value for the collection name.  In this case, all 
`Employee` instances will be saved in to the `employees` collection instead.  There is a little more to this annotation but the [javadoc]
(/javadoc) covers those details.

The `@Indexes` annotation lists which annotations morphia should create.  In this instance, we're defining an index named `salary` on the
 field salary with the default ordering of ascending.  More information on indexing can found [here]({{< relref "annotations.md#indexes" 
 >}}).
 
We've marked the `id` field to be used as our primary key (the `_id` field in the document).  In this instance we're using the Java driver 
type of `ObjectId` as the ID type.  The ID can be any type you'd like but is generally something like `ObjectId` or `Long`.  There are 
two other annotations to cover but it should be pointed out now that other than transient and static fields, morphia will attempt copy 
every field's value in to a document bound for the database.

The simplest of the two remaining annotations is `@Property`.  This annotation is entirely optional.  If you leave this annotation off, 
morphia will use the Java field name as the document field name.  Often times this is fine.  However, some times you'll want to change 
the document field name for any number of reasons.  In those cases, you can use `@Property` and pass it the name to be used when this 
class is serialized out to a document to be handed off to mongodb.  

This just leave `@Reference`.  This annotation is telling morphia that this field refers to other morphia mapped entities.  In this case 
morphia will store what mongodb calls a `DBRef` which is just a collection name and key value.  These referenced entities must already be
 saved or at least have an ID assigned or morphia will error out.
 
## Saving Data

For the most part, you treat your Java objects just like you normally would.  When you're ready to write an object to the database, it's 
a one liner:

```java
final Employee employee = new Employee("Elmer Fudd", 50000.0);
datastore.save(employee);
```

Taking it one step further, lets define some relationships and save those, too.

```java
```