package dev.morphia.testmappackage.testmapsubpackage.testmapsubsubpackage;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntityInSubSubPackage {
    @Id
    private ObjectId id;

    private String name;
}
