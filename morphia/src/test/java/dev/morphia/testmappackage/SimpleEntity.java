package dev.morphia.testmappackage;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntity {
    @Id
    private ObjectId id;

    private String name;
}
