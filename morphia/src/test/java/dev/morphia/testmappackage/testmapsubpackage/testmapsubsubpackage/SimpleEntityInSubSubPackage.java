package dev.morphia.testmappackage.testmapsubpackage.testmapsubsubpackage;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntityInSubSubPackage {
    @Id
    private ObjectId id;

    private String name;
}
