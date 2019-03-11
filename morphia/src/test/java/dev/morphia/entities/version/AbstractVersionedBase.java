package dev.morphia.entities.version;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;

@Entity
@SuppressWarnings("unused")
public abstract class AbstractVersionedBase {
    @Id
    private ObjectId id;
    @Version
    private long version;
}
