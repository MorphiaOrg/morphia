package dev.morphia.test.records;

import java.util.List;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.records.AotRecordAddress;
import dev.morphia.test.models.records.AotRecordHolder;
import dev.morphia.test.models.records.AotRecordPerson;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;

/**
 * Round-trips Java records through the datastore. The {@code Aot*} fixtures live in a package listed in
 * {@code morphia.packages}, so critter-maven pre-generates their models; the {@code Runtime*} fixtures live here, outside those
 * packages, so the critter mapper generates their models at runtime. Under the reflection mapper both sets use reflection.
 */
public class TestRecordEntities extends TestBase {
    private enum Tier {
        AOT,
        RUNTIME
    }

    @Test
    public void aotRecordEntity() {
        AotRecordPerson person = new AotRecordPerson(new ObjectId(), "Ada", 36, List.of("Countess", "Enchantress"));
        getDs().save(person);

        assertTier(AotRecordPerson.class, Tier.AOT);
        assertStoredShape(getDs().getCollection(AotRecordPerson.class).getNamespace().getCollectionName(), person.id());
        Assertions.assertEquals(person, getDs().find(AotRecordPerson.class).filter(eq("_id", person.id())).first());
        Assertions.assertEquals(person, getDs().find(AotRecordPerson.class).filter(eq("age", 36)).first());
    }

    @Test
    public void runtimeRecordEntity() {
        RuntimeRecordPerson person = new RuntimeRecordPerson(new ObjectId(), "Grace", 85, List.of("Amazing Grace"));
        getDs().save(person);

        assertTier(RuntimeRecordPerson.class, Tier.RUNTIME);
        assertStoredShape(getDs().getCollection(RuntimeRecordPerson.class).getNamespace().getCollectionName(), person.id());
        Assertions.assertEquals(person, getDs().find(RuntimeRecordPerson.class).filter(eq("_id", person.id())).first());
        Assertions.assertEquals(person, getDs().find(RuntimeRecordPerson.class).filter(eq("age", 85)).first());
    }

    @Test
    public void aotEmbeddedRecord() {
        AotRecordHolder holder = new AotRecordHolder(new AotRecordAddress("1 Main St", "Springfield"),
                List.of(new AotRecordAddress("2 Elm St", "Shelbyville"), new AotRecordAddress("3 Oak St", "Capital City")));
        getDs().save(holder);

        assertTier(AotRecordHolder.class, Tier.AOT);
        assertTier(AotRecordAddress.class, Tier.AOT);
        Assertions.assertEquals(holder, getDs().find(AotRecordHolder.class).filter(eq("_id", holder.getId())).first());
    }

    @Test
    public void runtimeEmbeddedRecord() {
        RuntimeRecordHolder holder = new RuntimeRecordHolder(new RuntimeRecordAddress("1 Main St", "Springfield"),
                List.of(new RuntimeRecordAddress("2 Elm St", "Shelbyville"), new RuntimeRecordAddress("3 Oak St", "Capital City")));
        getDs().save(holder);

        assertTier(RuntimeRecordHolder.class, Tier.RUNTIME);
        assertTier(RuntimeRecordAddress.class, Tier.RUNTIME);
        Assertions.assertEquals(holder, getDs().find(RuntimeRecordHolder.class).filter(eq("_id", holder.getId())).first());
    }

    /**
     * Fails if the critter mapper silently fell back to a different tier, which would leave the tier this test is named for
     * untested.
     */
    private void assertTier(Class<?> type, Tier tier) {
        EntityModel model = getMapper().getEntityModel(type);
        Assertions.assertNotNull(model, "No model for " + type.getName());
        if (morphiaConfig.mapper() == MapperType.REFLECTION) {
            Assertions.assertFalse(model instanceof CritterEntityModel,
                    "Expected a reflection model for " + type.getName() + " but got " + model.getClass().getName());
            return;
        }
        Assertions.assertInstanceOf(CritterEntityModel.class, model,
                "Expected a critter model for " + type.getName() + " but got " + model.getClass().getName());
        // Builds that don't run critter-maven's generate-test-models (e.g. pull-request CI) have no pre-generated
        // models, so an AOT fixture is generated at runtime there. Expect AOT exactly when its model is on the classpath.
        Tier expected = tier == Tier.AOT && !hasPregeneratedModel(type) ? Tier.RUNTIME : tier;
        boolean runtimeGenerated = model.getClass().getClassLoader() instanceof CritterClassLoader;
        Assertions.assertEquals(expected == Tier.RUNTIME, runtimeGenerated,
                "Expected a " + expected + " critter model for " + type.getName() + " but got " + model.getClass().getName()
                        + " from " + model.getClass().getClassLoader());
    }

    /**
     * @return true if critter-maven pre-generated a model for {@code type}, using the name {@code CritterMapper} looks up
     */
    private static boolean hasPregeneratedModel(Class<?> type) {
        String modelClass = Critter.critterPackage(type) + "." + type.getSimpleName() + "EntityModel";
        return type.getClassLoader().getResource(modelClass.replace('.', '/') + ".class") != null;
    }

    private void assertStoredShape(String collection, ObjectId id) {
        Document stored = getDatabase().getCollection(collection).find(new Document("_id", id)).first();
        Assertions.assertNotNull(stored, "Nothing stored in " + collection);
        Assertions.assertTrue(stored.containsKey("years"), "Expected the renamed component to be stored as 'years': " + stored);
        Assertions.assertFalse(stored.containsKey("age"), "Expected no 'age' key: " + stored);
    }
}
