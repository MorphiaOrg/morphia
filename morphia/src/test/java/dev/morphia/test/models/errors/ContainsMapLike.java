package dev.morphia.test.models.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
public class ContainsMapLike {
    public final MapLike m = new MapLike();
    @Id
    private ObjectId id;
}
