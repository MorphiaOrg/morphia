package dev.morphia.query.updates;

import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.test.mapping.codec.harness.models.HarnessRenames;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link UpdateOperator#field()} translates its path against the current mapping every time, so it follows changes to its
 * model and to the mapping itself.
 */
public class TestUpdateOperatorField {
    @Test
    void retranslatesAfterASubtypeIsMapped() {
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                    .database("update_operator_field")
                    .applyIndexes(false)
                    .applyCaps(false)
                    .applyDocumentValidations(false));
            EntityModel shape = datastore.getMapper().getEntityModel(Shape.class);
            UpdateOperator operator = UpdateOperators.inc("radius").datastore(datastore).model(shape).validate(false);
            Assertions.assertEquals("radius", operator.field(), "Circle isn't mapped yet");

            // as when encoding an earlier update's value maps a subtype for the first time
            datastore.getMapper().getEntityModel(Circle.class);
            Assertions.assertEquals("r", operator.field());
        }
    }

    @Test
    void retranslatesWhenTheModelChanges() {
        // nothing here talks to a server
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                    .database("update_operator_field")
                    .packages(List.of(HarnessRenames.class.getPackageName()))
                    .applyIndexes(false)
                    .applyCaps(false)
                    .applyDocumentValidations(false));
            EntityModel renames = datastore.getMapper().getEntityModel(HarnessRenames.class);
            UpdateOperator operator = UpdateOperators.set("name", "value").datastore(datastore);

            operator.model(renames);
            Assertions.assertEquals("n", operator.field());
            Assertions.assertEquals("n", operator.field());

            operator.model(null);
            Assertions.assertEquals("name", operator.field(), "no model, so nothing to translate against");

            operator.entityModel(renames);
            Assertions.assertEquals("n", operator.field());
        }
    }

    @Entity("shapes")
    abstract static class Shape {
        @Id
        ObjectId id;
    }

    @Entity("shapes")
    static class Circle extends Shape {
        @Property("r")
        double radius;
    }
}
