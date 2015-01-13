# Morphia

[![Build Status](https://jenkins.10gen.com/job/morphia/badge/icon)](https://jenkins.10gen.com/job/morphia/)

Morphia is a lightweight type-safe library for mapping Java objects to/from [MongoDB](http://www.mongodb.org/).  Morphia provides a
typesafe, and fluent [Query](https://github.com/mongodb/morphia/wiki/Query) API support with (runtime) validation.  Morphia
uses annotations so there are no XML files to manage or update.  Morphia should feel very comfortable for any developer with JPA
experience.


##Features
- [Lifecycle Method/Event](https://github.com/mongodb/morphia/wiki/LifecycleMethods) Support
- Works great with Guice, Spring, and other DI frameworks.
- Many extension points (new annotations, converters, mapping behavior, logging, etc.)
- Does not store Null/Empty values (by default).
- GWT support (entities are just POJOs) -- (GWT ignores annotations)
- Advanced mapper which allows raw conversion, `DBObject toDBObject(Object entity)` or `T fromDBObject(Class<T> entityClass, DBObject dbObject)`

Please continue by reading the QuickStart or looking at a list of [the annotations](https://github.com/mongodb/morphia/wiki/AllAnnotations).
If you have further questions, please reach out to us on our [mailing list](https://groups.google.com/forum/#!forum/morphia).

## Quick start

### Including morphia in your build
**Maven**

```xml
<dependency>
    <groupId>org.mongodb.morphia</groupId>
    <artifactId>morphia</artifactId>
    <version>###</version>
</dependency>
```

See the [dependencies](https://github.com/mongodb/morphia/wiki/Dependencies) page for more detail.

### Sample code
```java
@Entity("employees")
class Employee {
  // auto-generated, if not set (see ObjectId)
  @Id ObjectId id;

  // value types are automatically persisted
  String firstName, lastName;

  // only non-null values are stored
  Long salary = null;

  // by default fields are @Embedded
  Address address;

  //references can be saved without automatic loading
  Key<Employee> manager;

  //refs are stored**, and loaded automatically
  @Reference List<Employee> underlings = new ArrayList<Employee>();

  // stored in one binary field
  @Serialized EncryptedReviews encryptedReviews;

  //fields can be renamed
  @Property("started") Date startDate;
  @Property("left") Date endDate;

  //fields can be indexed for better performance
  @Indexed boolean active = false;

  //fields can loaded, but not saved
  @NotSaved String readButNotStored;

  //fields can be ignored (no load/save)
  @Transient int notStored;

  //not @Transient, will be ignored by Serialization/GWT for example.
  transient boolean stored = true;

  //Lifecycle methods -- Pre/PostLoad, Pre/PostPersist...
  @PostLoad void postLoad(DBObject dbObj) { ... }
}

...

Datastore ds = ...; // like new Morphia(new Mongo()).createDatastore("hr")
morphia.map(Employee.class);

ds.save(new Employee("Mister", "GOD", null, 0));

// get an employee without a manager
Employee boss = ds.find(Employee.class).field("manager").equal(null).get();

Key<Employee> scottsKey =
  ds.save(new Employee("Scott", "Hernandez", ds.getKey(boss), 150**1000));

//add Scott as an employee of his manager
UpdateResults<Employee> res =
  ds.update(
    boss,
    ds.createUpdateOperations(Employee.class).add("underlings", scottsKey)
  );

// get Scott's boss; the same as the one above.
Employee scottsBoss =
  ds.find(Employee.class).filter("underlings", scottsKey).get();

for (Employee e : ds.find(Employee.class, "manager", boss))
   print(e);
```

**Note**: @Reference will not save objects, just a reference to them; You must save them yourself.
