package dev.morphia.entities;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import org.bson.types.ObjectId;

@Entity
public class NamedIndexOnValue {
    @Id
    private ObjectId id;
    @Indexed(options = @IndexOptions(name = "value_ascending"))
    private final long value = 4;
}
