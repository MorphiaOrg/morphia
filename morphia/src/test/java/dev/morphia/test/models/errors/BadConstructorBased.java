package dev.morphia.test.models.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.experimental.Constructor;
import dev.morphia.annotations.experimental.Name;
import org.bson.types.ObjectId;

@Entity
public class BadConstructorBased {
    @Id
    private final ObjectId id;
    private final String name;

    @Constructor
    public BadConstructorBased(@Name("_id") ObjectId id,
                               @Name("named") String name) {
        this.id = id;
        this.name = name;
    }
}
