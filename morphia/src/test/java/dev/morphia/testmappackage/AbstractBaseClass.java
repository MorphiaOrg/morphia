package dev.morphia.testmappackage;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
@SuppressWarnings("unused")
public abstract class AbstractBaseClass {
    @Id
    private ObjectId id;
}
