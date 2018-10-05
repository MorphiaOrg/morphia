package xyz.morphia.entities.version;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Version;

@Entity
@SuppressWarnings("unused")
public abstract class AbstractVersionedBase {
    @Id
    private ObjectId id;
    @Version
    private long version;
}
