package org.mongodb.morphia.testmappackage;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

@SuppressWarnings("UnusedDeclaration")
public class ClassWithoutEntityAnnotation {
    @Id
    private ObjectId id;

    private String name;
}
