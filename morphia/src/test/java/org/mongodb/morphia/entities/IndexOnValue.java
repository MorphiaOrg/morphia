package org.mongodb.morphia.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

public class IndexOnValue {
    @Id
    private ObjectId id;
    @Indexed
    private long value = 4;
}
