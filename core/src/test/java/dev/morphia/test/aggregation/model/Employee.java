package dev.morphia.test.aggregation.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity(value = "employees", useDiscriminator = false)
public class Employee {
    @Id
    private ObjectId id;
}
