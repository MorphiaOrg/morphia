package xyz.morphia.entities;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Indexed;

public class IndexOnValue {
    @Id
    private ObjectId id;
    @Indexed
    private long value = 4;
}
