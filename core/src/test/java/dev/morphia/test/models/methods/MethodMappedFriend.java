package dev.morphia.test.models.methods;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.Objects;

@Entity
public class MethodMappedFriend {
    private ObjectId id;

    @Id
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MethodMappedFriend)) {
            return false;
        }
        MethodMappedFriend that = (MethodMappedFriend) o;
        return Objects.equals(id, that.id);
    }
}
