package dev.morphia.test.models.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@Entity
public class ContainsXKeyMap<T> {
    public final Map<T, String> values = new HashMap<>();
    @Id
    public ObjectId id;

    @Override
    public String toString() {
        return new StringJoiner(", ", ContainsXKeyMap.class.getSimpleName() + "[", "]")
                   .add("id=" + id)
                   .add("values=" + values)
                   .toString();
    }
}
