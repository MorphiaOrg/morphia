package org.mongodb.morphia.callbacks;


import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostPersist;


public class ProblematicPostPersistEntity {
  @Id ObjectId id;

  final Inner i = new Inner();

  boolean called;

  @PostPersist void m1() {
    called = true;
  }

  static class Inner {
    boolean called;

    String foo = "foo";

    @PostPersist void m2() {
      called = true;
    }
  }
}
