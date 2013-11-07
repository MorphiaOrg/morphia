package org.mongodb.morphia.ext.entityscanner;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity
class F {
  @Id
  private ObjectId id;
}