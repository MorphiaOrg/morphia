package dev.morphia.test.mapping.lazy;

import dev.morphia.annotations.Entity;
import org.bson.types.ObjectId;

@Entity
public interface InterfaceB {
    ObjectId getId();
}
