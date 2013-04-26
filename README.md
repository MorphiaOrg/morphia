# Morphia

**Morphia** is a lightweight type-safe library for mapping Java objects to/from [MongoDB](http://www.mongodb.org/):

- Easy to use, and very **lightweight**; reflection is used once per type and cached for **good performance**.
- [Datastore](https://github.com/mongodb/morphia/wiki/Datastore) and [DAO](https://github.com/mongodb/morphia/wiki/DAOSupport)<T,V> access abstractions, or roll your own...
- **Type-safe**, and Fluent [Query](https://github.com/mongodb/morphia/wiki/Query) support with (runtime) validation
- **Annotations** based mapping behavior; there are no XML files.
- Extensions: [Validation (jsr303)](https://github.com/mongodb/morphia/wiki/ValidationExtension), and [SLF4J Logging](https://github.com/mongodb/morphia/wiki/SLF4JExtension)

```java
@Entity("employees")
class Employee {
  @Id ObjectId id; // auto-generated, if not set (see ObjectId)
  String firstName, lastName; // value types are automatically persisted
  Long salary = null; // only non-null values are stored 

  Address address; // by default fields are @Embedded

  Key<Employee> manager; //references can be saved without automatic loading
  @Reference List<Employee> underlings = new ArrayList<Employee>(); //refs are stored*, and loaded automatically

  @Serialized EncryptedReviews; // stored in one binary field 
 
  @Property("started") Date startDate; //fields can be renamed
  @Property("left") Date endDate;

  @Indexed boolean active = false; //fields can be indexed for better performance
  @NotSaved string readButNotStored; //fields can loaded, but not saved
  @Transient int notStored; //fields can be ignored (no load/save)
  transient boolean stored = true; //not @Transient, will be ignored by Serialization/GWT for example.

  //Lifecycle methods -- Pre/PostLoad, Pre/PostPersist...
  @PostLoad void postLoad(DBObject dbObj) { ... }
}

...

Datastore ds = ...; // like new Morphia(new Mongo()).createDatastore("hr")
morphia.map(Employee.class);

ds.save(new Employee("Mister", "GOD", null, 0));

Employee boss = ds.find(Employee.class).field("manager").equal(null).get(); // get an employee without a manager

Key<Employee> scottsKey = ds.save(new Employee("Scott", "Hernandez", ds.getKey(boss), 150*1000));

//add Scott as an employee of his manager
UpdateResults<Employee> res = ds.update(boss, ds.createUpdateOperations(Employee.class).add("underlings", scottsKey)); 

// get Scott's boss; the same as the one above.
Employee scottsBoss = ds.find(Employee.class).filter("underlings", scottsKey).get(); 

for (Employee e : ds.find(Employee.class, "manager", boss))
   print(e);
```

- [Lifecycle Method/Event](https://github.com/mongodb/morphia/wiki/LifecycleMethods) Support
- Works great with Guice, Spring, and other DI frameworks.
- Many extension points (new annotations, converters, mapping behavior, logging, etc.)
- Does not store Null/Empty values (by default).
- GWT support (entities are just POJOs) -- (GWT ignores annotations)
- Advanced mapper which allows raw conversion, void toObject(DBObject) or DBObject fromObject(Object)

Please continue by reading the [QuickStart](https://github.com/mongodb/morphia/wiki/QuickStart) or looking at a list of AllAnnotation.

**Note**: @Reference will not save objects, just a reference to them; You must save them yourself.