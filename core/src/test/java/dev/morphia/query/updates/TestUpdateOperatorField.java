package dev.morphia.query.updates;

import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.test.mapping.codec.harness.models.HarnessRenames;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link UpdateOperator#field()} caches the translated path, so it must start over whenever the model it was translated
 * against changes.
 */
public class TestUpdateOperatorField {
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
}
