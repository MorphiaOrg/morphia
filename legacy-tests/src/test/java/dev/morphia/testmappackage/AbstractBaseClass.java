package dev.morphia.testmappackage;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
@SuppressWarnings("unused")
public abstract class AbstractBaseClass {
    @Id
    private ObjectId id;
}
