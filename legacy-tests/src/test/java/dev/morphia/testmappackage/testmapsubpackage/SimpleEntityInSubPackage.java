package dev.morphia.testmappackage.testmapsubpackage;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntityInSubPackage {
    @Id
    private ObjectId id;

    private String name;
}
