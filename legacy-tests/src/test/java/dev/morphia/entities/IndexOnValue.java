package dev.morphia.entities;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import org.bson.types.ObjectId;

@Entity
public class IndexOnValue {
    @Id
    private ObjectId id;
    @Indexed
    private final long value = 4;
}
