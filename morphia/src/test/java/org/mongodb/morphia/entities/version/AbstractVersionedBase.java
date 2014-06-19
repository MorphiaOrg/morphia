package org.mongodb.morphia.entities.version;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Version;

@Entity
@SuppressWarnings("unused")
public abstract class AbstractVersionedBase {
    @Id
    private ObjectId id;
    @Version
    private long version;
}
