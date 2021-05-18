package dev.morphia.test.models.versioned.subversioned;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;
import org.bson.types.ObjectId;

@Entity
public class VersionedToo {
    @Id
    private ObjectId id;
    @Version
    private Long version;
    private String name;
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ObjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVersion() {
        return version;
    }
}
