package org.mongodb.morphia.testmappackage;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity
@SuppressWarnings("unused")
public abstract class AbstractBaseClass {
    @Id
    private ObjectId id;
}
