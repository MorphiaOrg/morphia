package dev.morphia.entities;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;

@Entity
public class UniqueIndexOnValue {
    @Id
    private ObjectId id;

    @Indexed(name = "l_ascending", unique = true)
    private long value;

    @Indexed(options = @IndexOptions(unique = true))
    private long unique;

    private String name;

    public UniqueIndexOnValue() {
    }

    public UniqueIndexOnValue(final String name) {
        this.name = name;
    }

    public void setValue(final long value) {
        this.value = value;
    }

    public void setUnique(final long value) {
        this.unique = value;
    }
}
