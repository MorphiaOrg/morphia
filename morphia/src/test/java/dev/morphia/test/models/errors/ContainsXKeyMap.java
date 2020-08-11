package dev.morphia.test.models.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

@Entity
public class ContainsXKeyMap<T> {
    public final Map<T, String> values = new HashMap<>();
    @Id
    public ObjectId id;
}
