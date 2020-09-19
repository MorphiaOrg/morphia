package dev.morphia.entities;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import org.bson.types.ObjectId;

@Entity
public class UniqueIndexOnValue {
    @Id
    private ObjectId id;

    @Indexed(options = @IndexOptions(name = "l_ascending", unique = true))
    private long value;

    @Indexed(options = @IndexOptions(unique = true))
    private long unique;

    private String name;

    public UniqueIndexOnValue() {
    }

    public UniqueIndexOnValue(String name) {
        this.name = name;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void setUnique(long value) {
        this.unique = value;
    }
}
