package dev.morphia.entities;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

public class IndexOnValue {
    @Id
    private ObjectId id;
    @Indexed
    private long value = 4;
}
