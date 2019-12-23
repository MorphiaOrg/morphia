package dev.morphia.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
public class StringDates {
    @Id
    private ObjectId id;
    private String string;
}
