package org.mongodb.morphia.ext.entityscanner;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity
class E {
  @Id
  private ObjectId id;
}