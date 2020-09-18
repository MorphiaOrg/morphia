package dev.morphia.test.models;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity(cap = @CappedAt(count = 1))
public class CurrentStatus {
    @Id
    public ObjectId id;
    public String message;

    private CurrentStatus() {
    }

    public CurrentStatus(final String msg) {
        message = msg;
    }
}
