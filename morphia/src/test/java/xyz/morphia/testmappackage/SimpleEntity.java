package xyz.morphia.testmappackage;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntity {
    @Id
    private ObjectId id;

    private String name;
}
