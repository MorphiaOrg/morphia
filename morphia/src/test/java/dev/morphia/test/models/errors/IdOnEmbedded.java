package dev.morphia.test.models.errors;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Embedded
public class IdOnEmbedded {
    @Id
    private ObjectId id;
}
