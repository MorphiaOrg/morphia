package dev.morphia.mapping.codec.pojo;

import java.util.Arrays;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.LoadOnly;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperType;
import dev.morphia.test.mapping.codec.harness.models.HarnessRenames;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The plan's encode order holds every property an encoder may write, in model order: everything but the id, which is
 * written first on its own, and {@code @LoadOnly} properties, which no encoder writes.
 */
public class TestCodecPlanEncodeOrder {
    @ParameterizedTest
    @EnumSource(MapperType.class)
    void leavesOutTheIdAndLoadOnlyProperties(MapperType mapper) {
        // nothing here talks to a server
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                    .database("codec_plan_encode_order")
                    .mapper(mapper)
                    .packages(List.of(HarnessRenames.class.getPackageName()))
                    .discriminator(DiscriminatorFunction.className())
                    .applyIndexes(false)
                    .applyCaps(false)
                    .applyDocumentValidations(false));
            CodecPlan plan = ((MorphiaCodec<?>) datastore.getCodecRegistry().get(HarnessRenames.class)).getPlan();
            EntityModel model = plan.model();

            List<PropertyModel> expected = model.getProperties().stream()
                    .filter(property -> property != model.getIdProperty() && !property.hasAnnotation(LoadOnly.class))
                    .toList();
            Assertions.assertEquals(expected, Arrays.asList(plan.encodeOrder()));
            Assertions.assertTrue(Arrays.stream(plan.encodeOrder()).noneMatch(property -> property.getName().equals("legacy")),
                    mapper + ": the @LoadOnly property is left out");
            Assertions.assertEquals(model.getProperties().size() - 2, plan.encodeOrder().length, mapper.toString());
        }
    }
}
