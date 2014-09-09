package org.mongodb.morphia.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Entity
public class UniqueIndexOnValue {
    @Id
    private ObjectId id;

    @Indexed(name = "l_ascending", unique = true)
    private long value = 4;

    private String name;
    
    public UniqueIndexOnValue() {
    }

    public UniqueIndexOnValue(final String name) {
        this.name = name;
    }

    public void setValue(final long value) {
        this.value = value;
    }
}
