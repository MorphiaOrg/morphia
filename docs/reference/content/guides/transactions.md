+++
title = "Transactions"
[menu.main]
  parent = "Reference Guides"
  pre = "<i class='fa fa-file-text-o'></i>"
+++

Starting with MongoDB version 4.0, multi-document transactions are now supported on replica sets.  Morphia 2.0 introduces a simple
 mechanism to access this functionality.  Morphia 2.0 adds the methods `withTransaction(MorphiaTransaction<T> transaction)` and 
 `withTransaction(MorphiaTransaction<T> transaction, ClientSessionOptions options)` allowing for the execution of logic scoped to a
  transaction.   
  
The API is designed to work with Java 8's lambda syntax for the most convenience.  In this example, let's assume we're building a
 shopping site.  
 
```java
User account = datastore.withTransaction((session) -> {
    User user = new User("jimbo", "jimhalpert@dundermifflin.com");
    user.setHomeAddress(new Address("123 Paper Lane", "Scranton", "PA", "18510"));

    return session.save(user);
});
``` 

In this simple example, we're starting with our standard `Datastore` and calling `withTransaction()`.  The lambda we're passing in
 executes all with the scope of a single transaction.  You'll note the single parameter passed in is a `MorphiaSession`.  This is
  actually a `Datastore` but it has been bound to a session.  Any changes to be made within the transaction should be made using this
   `session` reference.  Once the lambda returns, the transaction is automatically committed and the session closed.  If you need access
    to server session or the transaction, there are methods on `MorphiaSession` to return either of those.
    
Of course, it's not always possible to wrap things up neatly inside a lambda so let's take a look at more hands on approach:

```java
MorphiaSession session = datastore.startSession();
session.startTransaction();
User account = new User("jimbo", "jimhalpert@dundermifflin.com");
user.setHomeAddress(new Address("123 Paper Lane", "Scranton", "PA", "18510"));
session.save(user);
session.commitTransaction();
session.close();
``` 

This is essentially the same logic as above but now we're manually managing the transactional boundaries.  `MorphiaSession` is also
 `AutoCloseable` so you could wrap the entire block in a `try-with-resources` block and let that manage the session boundary for you:
 
```java
try(MorphiaSession session = datastore.startSession()) {
    session.startTransaction();
    User account = new User("jimbo", "jimhalpert@dundermifflin.com");
    user.setHomeAddress(new Address("123 Paper Lane", "Scranton", "PA", "18510"));
    session.save(user);
    session.commitTransaction();
}
```
