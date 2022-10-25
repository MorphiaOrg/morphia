package dev.morphia.test.aggregation.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import org.bson.types.ObjectId;

@Entity
public class Martian {
    @Id
    public ObjectId id;
    public String name;
}
