package dev.morphia.test.models.errors;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.Document;
import org.bson.types.ObjectId;

@Entity
public class ContainsDocument {
    private final Document document = new Document("field", "val");
    @Id
    private ObjectId id;
}
