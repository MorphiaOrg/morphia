package dev.morphia.test;

import com.mongodb.client.model.ReturnDocument;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.Morphia;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.versioned.AbstractVersionedBase;
import dev.morphia.test.models.versioned.Versioned;
import dev.morphia.test.models.versioned.VersionedChildEntity;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class TestVersioning extends TestBase {

    @Test
    public void testBulkUpdate() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        datastore.save(entity);

        entity = datastore.find(Versioned.class).filter(eq("_id", entity.getId())).first();
        assertEquals(entity.getName(), "Value 1");
        assertEquals(entity.getVersion().longValue(), 1);

        entity.setName("Value 2");
        datastore.save(entity);

        entity = datastore.find(Versioned.class).filter(eq("_id", entity.getId())).first();
        assertEquals(entity.getName(), "Value 2");
        assertEquals(entity.getVersion().longValue(), 2);

        Query<Versioned> query = datastore.find(Versioned.class);
        query.filter(eq("id", entity.getId()));
        query.update(set("name", "Value 3"))
             .execute();

        entity = datastore.find(Versioned.class).filter(eq("_id", entity.getId())).first();
        assertEquals(entity.getName(), "Value 3");
        assertEquals(entity.getVersion().longValue(), 3);
    }

    @Test
    public void testCanMapAPackageContainingAVersionedAbstractBaseClass() {
        getMapper().mapPackage("dev.morphia.test.models.versioned");

        List<EntityModel> entities = getMapper().getMappedEntities();
        assertThat(entities.size(), is(3));
        List<Class<?>> list = new ArrayList<>();
        for (EntityModel entityModel : entities) {
            list.add(entityModel.getType());
        }
        assertTrue(list.contains(VersionedChildEntity.class));
        assertTrue(list.contains(AbstractVersionedBase.class));
    }

    @Test
    public void testCanMapAnEntityWithAnAbstractVersionedParent() {
        Datastore datastore = Morphia.createDatastore(getMongoClient(), TEST_DB_NAME);
        Mapper mapper = datastore.getMapper();
        mapper.map(VersionedChildEntity.class);

        List<EntityModel> mappedEntities = mapper.getMappedEntities();
        assertEquals(mappedEntities.size(), 2, mappedEntities.toString());
        List<Class<?>> list = new ArrayList<>();
        for (EntityModel entityModel : mappedEntities) {
            list.add(entityModel.getType());
        }
        assertTrue(list.contains(VersionedChildEntity.class));
        assertTrue(list.contains(AbstractVersionedBase.class));
    }

    @Test
    public void testFindAndModify() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        Query<Versioned> query = datastore.find(Versioned.class);
        query.filter(eq("name", "Value 1"));
        entity = query.modify(set("name", "Value 3"))
                      .execute(new ModifyOptions()
                                   .returnDocument(ReturnDocument.AFTER)
                                   .upsert(true));

        assertEquals(entity.getName(), "Value 3");
        assertEquals(entity.getVersion().longValue(), 1);
        assertNotNull(entity.getId());
    }

    @Test
    public void testIncVersionNotOverridingOtherInc() {
        final Versioned version1 = new Versioned();
        version1.setCount(0);
        getDs().save(version1);

        assertEquals(version1.getVersion(), Long.valueOf(1));
        assertEquals(version1.getCount(), 0);

        Query<Versioned> query = getDs().find(Versioned.class);
        query.filter(eq("_id", version1.getId()));
        query.update(inc("count"))
             .execute(new UpdateOptions().upsert(true));

        final Versioned version2 = getDs().find(Versioned.class)
                                          .filter(eq("_id", version1.getId()))
                                          .first();

        assertEquals(version2.getVersion(), Long.valueOf(2));
        assertEquals(version2.getCount(), 1);
    }

    @Test
    public void testInittedPrimitive() {
        getMapper().map(InittedPrimitive.class);
        InittedPrimitive Primitive = new InittedPrimitive();
        getDs().save(Primitive);

        InittedPrimitive first = getDs().find(InittedPrimitive.class)
                                        .first();

        assertEquals(first.hubba, Primitive.hubba);
    }

    @Test
    public void testInittedWrapper() {
        getMapper().map(InittedWrapper.class);
        InittedWrapper wrapper = new InittedWrapper();
        getDs().save(wrapper);

        InittedWrapper first = getDs().find(InittedWrapper.class)
                                      .first();

        assertEquals(first.hubba, wrapper.hubba);

        assertThrows(ConcurrentModificationException.class, () -> {
            InittedWrapper hasId = new InittedWrapper();
            hasId.id = new ObjectId();
            getDs().save(hasId);
        });
    }

    @Test
    public void testPrimitive() {
        getMapper().map(Primitive.class);
        Primitive Primitive = new Primitive();
        getDs().save(Primitive);

        Primitive first = getDs().find(Primitive.class)
                                 .first();

        assertEquals(first.hubba, Primitive.hubba);
    }

    @Test
    public void testThrowsExceptionWhenTryingToSaveAnOldVersion() {
        assertThrows(ConcurrentModificationException.class, () -> {
            getDs().find(Versioned.class).delete(new DeleteOptions().multi(true));
            // given
            final Versioned version1 = new Versioned();
            getDs().save(version1);
            final Versioned version2 = getDs().find(Versioned.class).filter(eq("_id", version1.getId())).first();
            getDs().save(version2);

            // when
            getDs().save(version1);
        });
    }

    @Test
    public void testUpdatesToVersionedFileAreReflectedInTheDatastore() {
        final Versioned version1 = new Versioned();
        version1.setName("foo");

        this.getDs().save(version1);

        final Versioned version1Updated = getDs().find(Versioned.class).filter(eq("_id", version1.getId())).first();
        version1Updated.setName("bar");

        this.getDs().merge(version1Updated);

        final Versioned versionedEntityFromDs = this.getDs().find(Versioned.class).filter(eq("_id", version1.getId())).first();
        assertEquals(version1Updated.getName(), versionedEntityFromDs.getName());
    }

    @Test
    public void testVersionNumbersIncrementWithEachSave() {
        final Versioned version1 = new Versioned();
        getDs().save(version1);
        assertEquals(version1.getVersion(), Long.valueOf(1));

        final Versioned version2 = getDs().find(Versioned.class).filter(eq("_id", version1.getId())).first();
        getDs().save(version2);
        assertEquals(version2.getVersion(), Long.valueOf(2));
    }

    @Test
    public void testVersionedInserts() {
        List<Versioned> list = asList(new Versioned(), new Versioned(), new Versioned(), new Versioned(), new Versioned());
        getDs().insert(list);
        for (Versioned versioned : list) {
            assertNotNull(versioned.getVersion());
        }
    }

    @Test
    public void testVersionedUpsert() {
        final Datastore datastore = getDs();
        datastore.find(Versioned.class).delete(new DeleteOptions().multi(true));

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        Query<Versioned> query = datastore.find(Versioned.class);
        query.filter(eq("name", "Value 1"));
        query.update(set("name", "Value 3"))
             .execute(new UpdateOptions().upsert(true));

        entity = datastore.find(Versioned.class).iterator(new FindOptions().limit(1)).tryNext();
        assertEquals(entity.getName(), "Value 3");
        assertEquals(entity.getVersion().longValue(), 1);
    }

    @Test
    public void testWrapper() {
        getMapper().map(Wrapper.class);
        Wrapper wrapper = new Wrapper();
        getDs().save(wrapper);

        Wrapper first = getDs().find(Wrapper.class)
                               .first();

        assertEquals(first.hubba, wrapper.hubba);
    }


    private static class InittedPrimitive extends TestEntity {
        @Version
        private final long hubba = 18;
        private String name;

        @Override
        public int hashCode() {
            return Objects.hash(name, hubba);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof InittedPrimitive)) {
                return false;
            }
            InittedPrimitive that = (InittedPrimitive) o;
            return hubba == that.hubba &&
                   Objects.equals(name, that.name);
        }
    }

    private static class InittedWrapper extends TestEntity {
        @Version
        private final Long hubba = 12L;
        private String name;

        @Override
        public int hashCode() {
            return Objects.hash(name, hubba);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof InittedWrapper)) {
                return false;
            }
            InittedWrapper that = (InittedWrapper) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(hubba, that.hubba);
        }

    }

    private static class Primitive extends TestEntity {
        private String name;
        @Version
        private long hubba;

        @Override
        public int hashCode() {
            return Objects.hash(name, hubba);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Primitive)) {
                return false;
            }
            Primitive primitive = (Primitive) o;
            return hubba == primitive.hubba &&
                   Objects.equals(name, primitive.name);
        }
    }

    private static class Wrapper extends TestEntity {
        private String name;
        @Version
        private Long hubba;

        @Override
        public int hashCode() {
            return Objects.hash(name, hubba);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Wrapper)) {
                return false;
            }
            Wrapper wrapper = (Wrapper) o;
            return Objects.equals(name, wrapper.name) &&
                   Objects.equals(hubba, wrapper.hubba);
        }
    }
}
