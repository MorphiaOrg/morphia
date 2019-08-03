package dev.morphia.entities;

import dev.morphia.annotations.Entity;
import org.bson.types.ObjectId;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

@Entity
public class IndexOnValue {
    @Id
    private ObjectId id;
    @Indexed
    private long value = 4;
}
