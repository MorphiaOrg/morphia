+++
date = "2015-03-17T15:36:56Z"
title = "Quick Tour"
[menu.main]
  parent = "Getting Started"
  identifier = "Quick Tour"
  weight = 2
  pre = "<i class='fa'></i>"
+++

Morphia wraps the MongoDB Java driver so some level of familiarity with using the driver can be helpful.  Morphia does its best to 
abstract much of that away but if something is confusing, please consult the Java driver [documentation](http://mongodb.github
.io/mongo-java-driver/) as well.

The following code snippets come from the __QuickTour.java__ example code
that can be found with the [Morphia source]({{< srcref "morphia/src/examples/java/org/mongodb/morphia/example/QuickTour.java">}}).

## Setting up Morphia

The following example shows how to create the initial Morphia instance.  Using this instance, you can configure various aspects of how
Morphia maps your entities and validates your queries.


```java
final Morphia morphia = new Morphia();

// tell Morphia where to find your classes
// can be called multiple times with different packages or classes
morphia.mapPackage("org.morphia.example");

// create the Datastore connecting to the default port on the local host
final Datastore datastore = morphia.createDatastore(new MongoClient(), "morphia_example");
datastore.ensureIndexes();
```

This snippet creates the Morphia instance we'll be using in our simple application.  The __Morphia__ class exists to configure the __Mapper__
 to be used and to define various system-wide defaults.  It is also what is used to create the __Datastore__ we'll be using.  The 
 __Datastore__ takes two parameters:  the __MongoClient__ used to connect to MongoDB and the name of the database to use.  With this 
 approach we could conceivably configure Morphia once and then connect to multiple databases by creating different __Datastore__ instances.
   In practice, this is likely pretty rare but it is possible.
   
The second line, which we skipped over, deserves a bit of consideration.  In this case, we're telling Morphia to look at every class in the 
package we've given and find every class annotated with __@Entity__ (which we'll cover shortly) and discover the mapping metadata we've 
put on our classes.  There are several variations of mapping that can be done and they can be called multiple times with different values
 to properly cover all your entities wherever they might live in your application.
 
## Mapping Options

Once you have an instance of Morphia, you can configure various mapping options via the __MappingOptions__ class.  While it's possible to 
specify the __Mapper__ when creating an instance of Morphia, most users will use the default mapper.  In either case, the __Mapper__ can
 be fetched using the __getMapper()__ method on the __Morphia__ instance.  The two most common elements to configure are __storeEmpties__ and
 __storeNulls__.  By default Morphia will not store empty __List__ or __Map__ values nor will it store null values in to MongoDB.  If your 
 application needs empty or null values to be present for whatever reason, setting these values to true will tell Morphia to save them 
 for you.  There are a few other options to configure on __MappingOptions__, but we'll not be covering them here.
 
## Mapping Classes

There are two ways that Morphia can handle your classes:  as top level entities or embedded in others.  Any class annotated with __@Entity__
 is treated as a top level document stored directly in a collection.  Any class with __@Entity__ must have a field annotated with __@Id__ to 
define which field to use as the ___id__ value in the document written to MongoDB.  __@Embedded__ indicates that the class will result in a 
subdocument inside another document.  __@Embedded__ classes do not require the presence of an __@Id__ field.

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

There are a few things here to discuss and others we'll defer to later sections.  This class is annotated using the __@Entity__ annotation 
so we know that it will be a top level document.  In the annotation, you'll see __"employees"__.  By default, Morphia will use the class 
name as the collection name.  If you pass a String instead, it will use that value for the collection name.  In this case, all 
__Employee__ instances will be saved in to the __employees__ collection instead.  There is a little more to this annotation but the 
[annotations guide]({{< relref "/guides/annotations.md#entity" >}}) covers those details.

The __@Indexes__ annotation lists which indexes Morphia should create.  In this instance, we're defining an index named __salary__ on the
 field salary with the default ordering of ascending.  More information on indexing can found
  [here]({{< relref "/guides/annotations.md#indexes" >}}).
 
We've marked the __id__ field to be used as our primary key (the ___id__ field in the document).  In this instance we're using the Java driver 
type of __ObjectId__ as the ID type.  The ID can be any type you'd like but is generally something like __ObjectId__ or __Long__.  There are 
two other annotations to cover but it should be pointed out now that other than transient and static fields, Morphia will attempt to copy 
every field to a document bound for the database.

The simplest of the two remaining annotations is __@Property__.  This annotation is entirely optional.  If you leave this annotation off, 
Morphia will use the Java field name as the document field name.  Often times this is fine.  However, some times you'll want to change 
the document field name for any number of reasons.  In those cases, you can use __@Property__ and pass it the name to be used when this 
class is serialized out to a document to be handed off to MongoDB.  

This just leaves __@Reference__.  This annotation is telling Morphia that this field refers to other Morphia mapped entities.  In this case 
Morphia will store what MongoDB calls a [__DBRef__]({{< docsref "reference/database-references/#dbrefs" >}}) which is just a 
collection name and key value.  These referenced entities must already be saved or at least have an ID assigned or Morphia will throw an
 exception.
 
## Saving Data

For the most part, you treat your Java objects just like you normally would.  When you're ready to write an object to the database, it's 
as simple as this:

```java
final Employee elmer = new Employee("Elmer Fudd", 50000.0);
datastore.save(elmer);
```

Taking it one step further, lets define some relationships and save those, too.

```java
final Employee daffy = new Employee("Daffy Duck", 40000.0);
datastore.save(daffy);

final Employee pepe = new Employee("Pepé Le Pew", 25000.0);
datastore.save(pepe);

elmer.getDirectReports().add(daffy);
elmer.getDirectReports().add(pepe);

datastore.save(elmer);
```

As you can see, we just need to create and save the other Employees then we can add them to the direct reports list and save.  Morphia 
takes care of saving the keys in Elmer's document that refer to Daffy and Pepé.  Updating data in MongoDB is as simple as updating your 
Java objects and then calling __datastore.save()__ with them again.  For bulk updates (e.g., everyone gets a raise!) this is not the most 
efficient way of doing updates.  It is possible to update directly in the database without having to pull in every document, convert to 
Java objects, update, convert back to a document, and write back to MongoDB.  But in order to show you that piece, first we need to see 
how to query.

## Querying

Morphia attempts to make your queries as type safe as possible.  All of the details of converting your data are handled by Morphia 
directly and only rarely do you need to take additional action.  As with everything else, __Datastore__ is where we start:

```java
final Query<Employee> query = datastore.createQuery(Employee.class);
final List<Employee> employees = query.asList();
```

This is a basic Morphia query.  Here, we're telling the __Datastore__ to create a query that's been typed to __Employee__.  In this 
case, we're fetching every __Employee__ in to a __List__.  For very large query results, this could very well be too much to fit in to 
memory.  For this simple example, using __asList()__ is fine but in practice __fetch()__ is usually the more appropriate choice.  Most queries 
will, of course, want to filter the data in some way. There are two ways of doing this:

```java
underpaid = datastore.createQuery(Employee.class)
                     .field("salary").lessThanOrEq(30000)
                     .asList();
```

The __field()__ method here is used to filter on the named field and returns an instance of an interface with a number of methods to build 
a query.  This approach is helpful if compile-time checking is needed.  Between javac failing on missing methods and IDE auto-completion,
 query building can be done quite safely.
 
 
The other approach uses the __filter()__ method which is a little more free form and succinct than __field()__.  Here we can embed 
certain operators in the query string.  While this is less verbose than the alternative, it does leave more things in the string to 
validate and potentially get wrong:

```java
List<Employee> underpaid = datastore.createQuery(Employee.class)
                                    .filter("salary <=", 30000)
                                    .asList();
```

Either query works.  It comes down to a question of preference in most cases.  In either approach, Morphia will validate that there is a 
field called __salary__ on the __Employee__ class.  If you happen to have mapped that field such that the name in the database doesn't match 
the Java field, Morphia can use either form and will validate against either name.

## Updates

Now that we can query, however simply, we can turn to in-database updates.  These updates take two components: a query, and a set 
of update operations.  In this example, we'll find all the underpaid employees and give them a raise of 10000.  The first step is to create
 the query to find all the underpaid employees.  This is one we've already seen:
 
```java
final Query<Employee> underPaidQuery = datastore.createQuery(Employee.class)
                                             .filter("salary <=", 30000);
```

To define how we want to update the documents matched by this query, we create an __UpdateOperations__ instance:

```java
final UpdateOperations<Employee> updateOperations = datastore.createUpdateOperations(Employee.class)
                                                   .inc("salary", 10000);
```

There are many operations on this class but, in this case, we're only updating the __salary__ field by 10000.  This corresponds to the 
[__$inc__]({{< docsref "reference/operator/update/inc/" >}}) operator.  There's one last step involved here:

```java
final UpdateResults results = datastore.update(underPaidQuery, updateOperations);
```

This line executes the update in the database without having to pull in however many documents are matched by the query.  The 
__UpdateResults__ instance returned will contain various statistics about the update operation.

## Removes

After everything else, removes are really quite simple.  Removing just needs a query to find and delete the documents in question and 
then tell the __Datastore__ to delete them:

```java
final Query<Employee> overPaidQuery = datastore.createQuery(Employee.class)
                                                .filter("salary >", 100000);
datastore.delete(overPaidQuery);
```

There are a couple of variations on __delete()__ but this is probably the most common usage.  If you already have an object in hand, there 
is a __delete__ that can take that reference and delete it.  There is more information in the [javadoc](/javadoc).
