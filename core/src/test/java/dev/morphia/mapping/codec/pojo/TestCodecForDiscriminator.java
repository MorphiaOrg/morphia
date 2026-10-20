package dev.morphia.mapping.codec.pojo;

import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.ManualMorphiaConfig;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperType;
import dev.morphia.test.mapping.codec.harness.models.HarnessCircle;
import dev.morphia.test.mapping.codec.harness.models.HarnessShape;
import dev.morphia.test.mapping.codec.harness.models.HarnessSquare;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks what {@link MorphiaCodec#codecForDiscriminator(String)} caches: nothing for its own discriminator, which it
 * answers without a lookup, and one entry per other discriminator however often it's asked.
 */
public class TestCodecForDiscriminator {
    @ParameterizedTest
    @EnumSource(MapperType.class)
    @SuppressWarnings("unchecked")
    void cachesOnlyOtherTypes(MapperType mapper) {
        // nothing here talks to a server
        try (MongoClient client = MongoClients.create("mongodb://localhost:1")) {
            MorphiaDatastore datastore = new MorphiaDatastore(client, ManualMorphiaConfig.configure()
                    .database("codec_for_discriminator")
                    .mapper(mapper)
                    .packages(List.of(HarnessShape.class.getPackageName()))
                    .discriminator(DiscriminatorFunction.className())
                    .applyIndexes(false)
                    .applyCaps(false)
                    .applyDocumentValidations(false));
            MorphiaCodec<HarnessShape> shapes = (MorphiaCodec<HarnessShape>) datastore.getCodecRegistry().get(HarnessShape.class);
            MorphiaCodec<?> circles = (MorphiaCodec<?>) datastore.getCodecRegistry().get(HarnessCircle.class);
            String circle = circles.getEntityModel().discriminator();
            String square = datastore.getMapper().getEntityModel(HarnessSquare.class).discriminator();

            for (int i = 0; i < 3; i++) {
                Assertions.assertSame(circles, circles.codecForDiscriminator(circle));
            }
            Assertions.assertTrue(circles.discriminatorCodecs.isEmpty(), mapper + ": a self-match needs no lookup");

            for (int i = 0; i < 3; i++) {
                shapes.codecForDiscriminator(circle);
                shapes.codecForDiscriminator(square);
            }
            Assertions.assertEquals(2, shapes.discriminatorCodecs.size(),
                    mapper + ": one cached entry per other discriminator: " + shapes.discriminatorCodecs.keySet());
            Assertions.assertSame(circles, shapes.discriminatorCodecs.get(circle));
        }
    }
}
