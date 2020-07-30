package dev.morphia.testmappackage;

import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@SuppressWarnings("UnusedDeclaration")
public class ClassWithoutEntityAnnotation {
    @Id
    private ObjectId id;

    private String name;
}
