package xyz.morphia.testmappackage;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;

@Entity
@SuppressWarnings("unused")
public abstract class AbstractBaseClass {
    @Id
    private ObjectId id;
}
