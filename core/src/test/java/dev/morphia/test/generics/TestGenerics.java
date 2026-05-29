package dev.morphia.test.generics;

import java.util.List;
import java.util.UUID;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.SpecializedEntity;
import dev.morphia.test.models.generics.Another;
import dev.morphia.test.models.generics.Child;
import dev.morphia.test.models.generics.ChildEntity;
import dev.morphia.test.models.methods.MethodMappedSpecializedEntity;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static java.util.Arrays.asList;
import static java.util.List.of;

public class TestGenerics extends TestBase {
    public TestGenerics() {
        super(buildConfig()
                .packages(of(Element.class.getPackageName())));
    }

    @Test
    public void example() {
        ChildEntity entity = new ChildEntity();
        entity.setEmbeddedList(asList(new Child("first"), new Child("second"), new Another("third")));
        getDs().save(entity);

        ChildEntity childEntity = getDs().find(ChildEntity.class)
                .iterator()
                .next();

        Assertions.assertEquals(entity, childEntity);
    }

    @Test
    public void testBoundGenerics() {
        getDs().getCollection(Element.class);
        getDs().getCollection(AudioElement.class);
    }

    @Test
    public void testGenericEntities() {
        EntityModel entityModel = getMapper().getEntityModel(SpecializedEntity.class);

        PropertyModel test = entityModel.getProperty("test");
        Assertions.assertEquals(UUID.class, test.getType());

        SpecializedEntity beforeDB = new SpecializedEntity();
        beforeDB.setId(UUID.randomUUID());
        beforeDB.setTest(UUID.randomUUID());
        getDs().save(beforeDB);

        SpecializedEntity loaded = getDs().find(SpecializedEntity.class)
                .filter(eq("_id", beforeDB.getId()))
                .first();

        Assertions.assertEquals(beforeDB.getId(), loaded.getId());

        Assertions.assertEquals(beforeDB.getTest(), loaded.getTest());
    }

    @Test
    public void testIt() {
        final ContainsThings ct = new ContainsThings();
        final HoldsAnInteger hai = new HoldsAnInteger();
        hai.setThing(7);
        final HoldsAString has = new HoldsAString();
        has.setThing("tr");
        ct.stringThing = has;
        ct.integerThing = hai;

        getDs().save(ct);
        Assertions.assertNotNull(ct.id);
        Assertions.assertEquals(1, getDs().find(ContainsThings.class).count());
        final ContainsThings ctLoaded = getDs().find(ContainsThings.class).iterator()
                .next();
        Assertions.assertNotNull(ctLoaded);
        Assertions.assertNotNull(ctLoaded.id);
        Assertions.assertNotNull(ctLoaded.stringThing);
        Assertions.assertNotNull(ctLoaded.integerThing);
    }

    @Test
    public void testMethodMappedGenericEntities() {
        withConfig(buildConfig()
                .packages(of(MethodMappedSpecializedEntity.class.getPackageName()))
                .propertyDiscovery(PropertyDiscovery.METHODS), () -> {

                    EntityModel entityModel = getMapper().getEntityModel(MethodMappedSpecializedEntity.class);

                    PropertyModel test = entityModel.getProperty("test");
                    Assertions.assertEquals(UUID.class, test.getType());

                    MethodMappedSpecializedEntity beforeDB = new MethodMappedSpecializedEntity();
                    beforeDB.setId(UUID.randomUUID());
                    beforeDB.setTest(UUID.randomUUID());
                    getDs().save(beforeDB);

                    MethodMappedSpecializedEntity loaded = getDs().find(MethodMappedSpecializedEntity.class)
                            .filter(eq("_id", beforeDB.getId()))
                            .first();

                    Assertions.assertEquals(beforeDB.getId(), loaded.getId());

                    Assertions.assertEquals(beforeDB.getTest(), loaded.getTest());
                });

    }

    @Test
    public void upperBounds() {
        Status<EmailItem> status = new EmailStatus();
        status.items = of(new EmailItem("help@example.org"));

        getDs().save(status);

        Assertions.assertNotNull(getDs().find(EmailStatus.class).first());
    }

    @Test
    public void testWildCards() {
        EntityModel model = getMapper().getEntityModel(WildCards.class);
        Assertions.assertEquals(1, model.getProperties().size());
    }

    @Entity
    private interface Item {
    }

    private static class AudioElement extends Element<Long> {
    }

    @Entity
    private static class ContainsThings {
        @Id
        private String id;
        private HoldsAString stringThing;
        private HoldsAnInteger integerThing;
    }

    @Entity
    private abstract static class Element<T extends Number> {
        @Id
        private ObjectId id;
        private T[] resources;
    }

    @Entity
    private static class EmailItem implements Item {
        private String to;

        public EmailItem() {
        }

        public EmailItem(String to) {
            this.to = to;
        }
    }

    @Entity
    private static class EmailStatus extends Status<EmailItem> {

    }

    @Entity
    private static class GenericHolder<T> {
        @Property
        private T thing;

        public T getThing() {
            return thing;
        }

        public void setThing(T thing) {
            this.thing = thing;
        }
    }

    private static class HoldsAString extends GenericHolder<String> {
    }

    private static class HoldsAnInteger extends GenericHolder<Integer> {
    }

    @Entity
    private abstract static class Status<T extends Item> {
        @Id
        private ObjectId id;
        @Property
        private List<T> items;

    }

    @Entity
    private static class WildCards {
        private static final Class<? extends Status<EmailItem>> PROCEDURE_CLASS = EmailStatus.class;
        @Id
        private ObjectId id;
    }
}
