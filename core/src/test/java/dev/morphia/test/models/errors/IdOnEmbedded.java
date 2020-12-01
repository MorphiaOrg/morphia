package dev.morphia.test.models.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
public class IdOnEmbedded {
    @Id
    private ObjectId id;
}
