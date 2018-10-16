package xyz.morphia.entities;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Indexed;

@Entity
public class NamedIndexOnValue {
    @Id
    private ObjectId id;
    @Indexed(name = "value_ascending")
    private long value = 4;
}
