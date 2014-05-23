package org.mongodb.morphia.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Entity
public class NamedIndexOnValue {
    @Id
    private ObjectId id;
    @Indexed(name = "value_ascending")
    private long value = 4;
}
