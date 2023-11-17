package dev.morphia.test;

import dev.morphia.Datastore;
import dev.morphia.InsertOneOptions;
import dev.morphia.MorphiaDatastore;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.LoadOnly;
import dev.morphia.annotations.Version;
import dev.morphia.query.filters.Filters;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.set;
import static org.testng.Assert.assertNotNull;

public class TestDatastoreMerge extends TestBase {

    @Test
    public void merge() {
        Datastore ds = getDs();

        Test1 test1 = new Test1();
        test1.name = "foobar";
        ds.save(test1);

        Test2 test2 = ds.find(Test2.class).first();
        assertNotNull(test2.id);
        test2.blarg = "barfoo";
        long version = test2.version;
        ds.merge(test2);

        Assert.assertEquals(version + 1, test2.version);
        test1 = ds.find(Test1.class).filter(eq("_id", test1.id)).first();

        assertNotNull(test1.name);
    }

    @Test
    public void testMerge() {
        final Merger te = new Merger();
        te.name = "test1";
        te.foo = "bar";
        te.position = 1;
        getDs().save(te);

        Assert.assertEquals(getDs().find(te.getClass()).count(), 1);

        //only update the position field with merge, normally save would override the whole object.
        final Merger te2 = new Merger();
        te2.id = te.id;
        te2.position = 5;
        Merger merge = getDs().merge(te2);

        Assert.assertEquals(te.name, merge.name);
        Assert.assertEquals(te.foo, merge.foo);
        Assert.assertEquals(te2.position, merge.position);
    }

    @Test
    public void testMergeWithUnset() {
        final Merger te = new Merger();
        te.name = "test1";
        te.foo = "bar";
        te.position = 1;
        getDs().save(te);

        Assert.assertEquals(getDs().find(te.getClass()).count(), 1);

        //only update the position field with merge, normally save would override the whole object.
        final Merger te2 = new Merger();
        te2.id = te.id;
        te2.position = 5;
        Merger merge = getDs().merge(te2, new InsertOneOptions().unsetMissing(true));

        Assert.assertNull(merge.name);
        Assert.assertNull(merge.foo);
        Assert.assertEquals(te2.id, merge.id);
        Assert.assertEquals(te2.position, merge.position);
    }

    @Test
    public void testMergeWithUnsetAndLoadOnly() {
        // Saves base entity with a name:
        LoadOnlyEntity entity = new LoadOnlyEntity();
        entity.name = "TestA";
        MorphiaDatastore ds = getDs();
        ds.save(entity);

        // Updates the "load only" value through an update operation:
        ds.find(LoadOnlyEntity.class)
                .filter(Filters.eq("_id", entity.id))
                .update(new UpdateOptions(),
                        set("loadOnlyValue", "My Value"));
        LoadOnlyEntity updatedEntity1 = ds.find(LoadOnlyEntity.class)
                .filter(Filters.eq("_id", entity.id))
                .first();
        assertNotNull(updatedEntity1.loadOnlyValue); // PASSES

        // Updates the name value using the datastore's "merge()" method:
        entity.name = "TestB";
        ds.merge(entity, new InsertOneOptions().unsetMissing(true));
        LoadOnlyEntity updatedEntity2 = ds.find(LoadOnlyEntity.class)
                .filter(Filters.eq("_id", entity.id))
                .first();
        assertNotNull(updatedEntity2.loadOnlyValue); // FAILS
    }

    @Entity
    private static class LoadOnlyEntity {
        @Id
        private ObjectId id;

        private String name;
        @LoadOnly
        private String loadOnlyValue;
    }

    @Entity
    private static class Merger {
        @Id
        private ObjectId id;
        private String name;
        private String foo;
        private Integer position;
    }

    @Entity(value = "test", useDiscriminator = false)
    static class Test1 {
        @Id
        ObjectId id;
        String name;
        @Version
        long version;
    }

    @Entity(value = "test", useDiscriminator = false)
    static class Test2 {
        @Id
        ObjectId id;
        String blarg;
        @Version
        long version;
    }
}
