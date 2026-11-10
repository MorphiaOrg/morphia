package dev.morphia.test.query;

import java.util.List;
import java.util.stream.Stream;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.MorphiaQuery;
import dev.morphia.test.TestBase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

/**
 * Covers the discriminator filter a query adds for its entity type.
 */
public class TestQueryDiscriminators extends TestBase {
    @Test
    public void limitsToTheTypeAndItsSubtypesWhenPolymorphic() {
        withConfig(buildConfig(Animal.class, Dog.class, Puppy.class).enablePolymorphicQueries(true), () -> {
            Assertions.assertEquals(in(Animal.class, Dog.class, Puppy.class), queryDocument(Animal.class));
            Assertions.assertEquals(in(Dog.class, Puppy.class), queryDocument(Dog.class));
        });
    }

    @Test
    public void limitsToTheTypeAlone() {
        withConfig(buildConfig(Animal.class, Dog.class, Puppy.class).enablePolymorphicQueries(false), () -> {
            Assertions.assertEquals(in(Animal.class), queryDocument(Animal.class));
        });
    }

    @Test
    public void leavesQueriesByIdOrDiscriminatorAlone() {
        withConfig(buildConfig(Animal.class, Dog.class, Puppy.class).enablePolymorphicQueries(true), () -> {
            ObjectId id = new ObjectId();
            Assertions.assertEquals(new Document("_id", id), document(getDs().find(Animal.class).filter(eq("_id", id))));
            Document byDiscriminator = new Document("_t", discriminator(Dog.class));
            Assertions.assertEquals(byDiscriminator, document(getDs().find(Animal.class, byDiscriminator)));
        });
    }

    @Test
    public void addsNothingWithoutADiscriminator() {
        withTestConfig(List.of(Plain.class), () -> {
            Assertions.assertEquals(new Document(), queryDocument(Plain.class));
        });
    }

    private Document in(Class<?>... types) {
        return new Document("_t", new Document("$in", Stream.of(types).map(this::discriminator).toList()));
    }

    private String discriminator(Class<?> type) {
        return getMapper().getEntityModel(type).discriminator();
    }

    private Document queryDocument(Class<?> type) {
        return document(getDs().find(type));
    }

    private static Document document(dev.morphia.query.Query<?> query) {
        return ((MorphiaQuery<?>) query).toDocument();
    }

    @Entity("animals")
    public static class Animal {
        @Id
        private ObjectId id;
    }

    @Entity("animals")
    public static class Dog extends Animal {
    }

    @Entity("animals")
    public static class Puppy extends Dog {
    }

    @Entity(useDiscriminator = false)
    public static class Plain {
        @Id
        private ObjectId id;
    }
}
