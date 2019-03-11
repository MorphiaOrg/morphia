package dev.morphia.entities;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

@Entity
public class NamedIndexOnValue {
    @Id
    private ObjectId id;
    @Indexed(name = "value_ascending")
    private long value = 4;
}
