package dev.morphia.testmappackage;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntity {
    @Id
    private ObjectId id;

    private String name;
}
