package dev.morphia.entities.version;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;
import org.bson.types.ObjectId;

@Entity
public abstract class AbstractVersionedBase {
    @Id
    private ObjectId id;
    @Version
    private long version;
}
