package dev.morphia.testmappackage.testmapsubpackage;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntityInSubPackage {
    @Id
    private ObjectId id;

    private String name;
}
