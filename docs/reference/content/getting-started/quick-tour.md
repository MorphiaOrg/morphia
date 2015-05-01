+++
date = "2015-03-17T15:36:56Z"
title = "Quick Tour"
[menu.main]
  parent = "Getting Started"
  identifier = "Quick Tour"
  weight = 30
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
morphia.createDatastore(new MongoClient(), "morphia_example");
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
