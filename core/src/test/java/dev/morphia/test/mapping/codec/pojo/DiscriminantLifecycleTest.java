package dev.morphia.test.mapping.codec.pojo;

import java.util.stream.Stream;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.Transient;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.List.of;

/**
 * asserts some common behaviour between entities with or without lifecycle
 */
public class DiscriminantLifecycleTest extends TestBase {

    public DiscriminantLifecycleTest() {
        super(buildConfig()
                .packages(of("dev.morphia.test.mapping.codec.pojo")));
    }

    static Stream<Arguments> classes() {
        return Stream.of(
                Arguments.of(BaseLifecycleEntity.class, ChildLifecycleEntity.class),
                Arguments.of(BaseEntity.class, ChildEntity.class));
    }

    @ParameterizedTest
    @MethodSource("classes")
    public void testCorrectEntity(Class<?> baseClass, Class<?> childClass) {
        ObjectId id = saveChildEntity(childClass);
        Child saved = (Child) getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
        Assertions.assertTrue(childClass.isInstance(saved));
        Assertions.assertTrue(saved.getAudited());
        Assertions.assertEquals(saved.getEmbed().embeddedValue, "embedded");
    }

    @ParameterizedTest
    @MethodSource("classes")
    public void testWrongDiscriminator(Class<?> baseClass, Class<?> childClass) {
        Assertions.assertThrows(CodecConfigurationException.class, () -> {
            Document entity = new Document("_t", "Nonsense");
            ObjectId id = getDatabase().getCollection("entity").insertOne(entity).getInsertedId().asObjectId().getValue();
            getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
        });
    }

    @ParameterizedTest
    @MethodSource("classes")
    public void testNonEntityDiscriminator(Class<?> baseClass, Class<?> childClass) {
        Assertions.assertThrows(CodecConfigurationException.class, () -> {
            ObjectId id = saveChildEntity(NonEntity.class);
            getDs().find(baseClass).filter(Filters.eq("_id", id)).first();
        });
    }

    private ObjectId saveChildEntity(Class<?> entityClass) {
        Document entity = new Document("_t", entityClass.getName());
        Document embed = new Document("_t", Embed.class.getName());
        embed.put("embeddedValue", "embedded");
        entity.put("embed", embed);
        return getDatabase().getCollection("entity").insertOne(entity).getInsertedId().asObjectId().getValue();
    }

    interface Child {
        boolean getAudited();

        Embed getEmbed();
    }

    @Entity(value = "entity")
    static class BaseLifecycleEntity {
        @Id
        ObjectId id;
        @Transient
        boolean audited;

        @PostLoad
        void audit() {
            // audit entity
            audited = true;
        }
    }

    static class ChildLifecycleEntity extends BaseLifecycleEntity implements Child {
        Embed embed;

        @Override
        public boolean getAudited() {
            return audited;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }
    }

    @Entity(value = "entity")
    static class BaseEntity {
        @Id
        ObjectId id;
        @Transient
        boolean audited = true;
    }

    static class ChildEntity extends BaseEntity implements Child {
        Embed embed;

        @Override
        public boolean getAudited() {
            return audited;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }
    }

    @Entity
    static class Embed {
        String embeddedValue;
    }

    static class NonEntity implements Child {
        Embed embed;

        @Override
        public boolean getAudited() {
            return true;
        }

        @Override
        public Embed getEmbed() {
            return embed;
        }
    }
}