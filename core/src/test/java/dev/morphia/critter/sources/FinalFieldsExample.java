package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An entity whose mapped fields are {@code final}, which {@code putfield} can only write from a constructor.
 */
@Entity
public class FinalFieldsExample {
    @Id
    private ObjectId id;
    private final String code;
    private final int count;

    public FinalFieldsExample(String code, int count) {
        this.code = code;
        this.count = count;
    }

    public String getCode() {
        return code;
    }

    public int getCount() {
        return count;
    }
}
