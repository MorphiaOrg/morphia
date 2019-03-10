package dev.morphia.entities.version;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;

public class Versioned {
    @Id
    private ObjectId id;
    @Version
    private Long version;
    private String name;
    private int count = 0;

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public ObjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getVersion() {
        return version;
    }
}
