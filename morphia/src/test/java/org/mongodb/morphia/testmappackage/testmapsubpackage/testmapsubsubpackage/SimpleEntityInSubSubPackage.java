package org.mongodb.morphia.testmappackage.testmapsubpackage.testmapsubsubpackage;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity
@SuppressWarnings("UnusedDeclaration")
public class SimpleEntityInSubSubPackage {
    @Id
    private ObjectId id;

    private String name;
}
