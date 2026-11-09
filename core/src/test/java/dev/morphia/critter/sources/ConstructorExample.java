package dev.morphia.critter.sources;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

/**
 * An entity decoded through a private constructor, which also has a constructor decoding never chooses.
 */
@Entity
public class ConstructorExample {
    @Id
    private ObjectId id;
    private final String name;
    private final long size;

    private ConstructorExample(String name, long size) {
        if ("invalid".equals(name)) {
            throw new IllegalArgumentException("invalid name");
        }
        this.name = name;
        this.size = size;
    }

    ConstructorExample(int unrelated) {
        this("unrelated", unrelated);
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}
