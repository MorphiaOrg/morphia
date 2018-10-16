package xyz.morphia.testmappackage;

import org.bson.types.ObjectId;
import xyz.morphia.annotations.Id;

@SuppressWarnings("UnusedDeclaration")
public class ClassWithoutEntityAnnotation {
    @Id
    private ObjectId id;

    private String name;
}
