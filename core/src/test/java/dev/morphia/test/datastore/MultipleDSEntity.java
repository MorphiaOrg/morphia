package dev.morphia.test.datastore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity
public class MultipleDSEntity {
    @Id
    private ObjectId id;
    private String name;
    private int count;
}
