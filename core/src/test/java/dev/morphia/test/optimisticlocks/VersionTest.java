package dev.morphia.test.optimisticlocks;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.TestEntity;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;


public class VersionTest extends TestBase {
    @Test
    public void testConcurrentModification() {
        assertThrows(ConcurrentModificationException.class, () -> {
            getMapper().map(List.of(VersionedType.class));

            final VersionedType a = new VersionedType();
            assertEquals(a.version, 0);
            getDs().save(a);
            assertEquals(a.version, 1);

            Query<VersionedType> query = getDs().find(VersionedType.class)
                                                .filter(eq("_id", a.getId()));
            getDs().save(query.iterator().next());

            assertEquals(query.iterator().next().version, 2);

            getDs().save(a);
        });
    }

    @Test
    public void testFindAndModify() {
        final VersionedType initial = new VersionedType();
        Datastore ds = getDs();
        ds.save(initial);

        Query<VersionedType> query = ds.find(VersionedType.class)
                                       .filter(eq("id", initial.getId()));
        VersionedType postUpdate = query.modify(set("text", "some new value"))
                                        .execute();

        assertEquals(postUpdate.version, initial.version);
    }

    @Test
    public void testInvalidVersionUse() {
        assertThrows(ConstraintViolationException.class, () -> getMapper().map(InvalidVersionUse.class));
    }

    @Test
    public void testManuallyIdentified() {
        assertThrows(ConcurrentModificationException.class, () -> {

            final NamedVersion entity1 = new NamedVersion();
            final NamedVersion entity2 = new NamedVersion();

            ObjectId id = new ObjectId();
            entity1.setId(id);
            entity2.setId(id);

            getDs().save(entity1);
            getDs().save(entity2);
        });
    }

    @Test
    public void testMerge() {
        assertThrows(ConcurrentModificationException.class, () -> {
            final NamedVersion a = new NamedVersion();
            Assert.assertNull(a.v);
            getDs().save(a);

            a.text = " foosdfds ";
            final NamedVersion a2 = getDs().find(NamedVersion.class)
                                           .filter(eq("_id", a.getId()))
                                           .first();
            getDs().save(a2);

            getDs().merge(a);
        });
    }

    @Test
    public void testMultiSaves() {
        getMapper().map(List.of(VersionedType.class));
        List<VersionedType> initial = List.of(new VersionedType(), new VersionedType());

        getDs().save(initial);

        Query<VersionedType> query = getDs().find(VersionedType.class);
        getDs().save(query.iterator().toList());

        List<VersionedType> loaded = query.iterator().toList();

        for (int i = 0, loadedSize = loaded.size(); i < loadedSize; i++) {
            final VersionedType type = loaded.get(i);
            assertEquals(type.id, initial.get(i).id);
            assertEquals(type.version, initial.get(i).version + 1);
        }

        assertThrows(ConcurrentModificationException.class, () -> {
            getDs().save(initial);
        });
    }

    @Test
    public void testUpdate() {
        Datastore ds = getDs();
        List<VersionedType> initial = List.of(new VersionedType(), new VersionedType());
        ds.save(initial);

        UpdateResult results = ds.find(VersionedType.class)
                                 .update(set("text", "some new value"))
                                 .execute();
        assertEquals(results.getModifiedCount(), 1);
        List<VersionedType> postUpdate = ds.find(VersionedType.class)
                                           .filter(eq("text", "some new value"))
                                           .iterator(new FindOptions()
                                                         .sort(Sort.ascending("_id")))
                                           .toList();

        for (int i = 0, postUpdateSize = postUpdate.size(); i < postUpdateSize; i++) {
            final VersionedType versionedType = postUpdate.get(i);
            assertEquals(versionedType.version, initial.get(i).version + 1);
        }
    }

    @Test
    public void testVersionFieldNameContribution() {
        final MappedField mappedFieldByJavaField = getMapper().getMappedClass(NamedVersion.class).getMappedFieldByJavaField("v");
        assertEquals(mappedFieldByJavaField.getMappedFieldName(), "v");
    }

    @Test
    public void testVersionInHashcode() {
        getMapper().mapPackage("com.example");

        final VersionInHashcode model = new VersionInHashcode();
        model.data = "whatever";
        getDs().save(model);
        Assert.assertNotNull(model.version);
    }

    @Test
    public void testVersions() {
        final VersionedType a = new VersionedType();
        assertEquals(a.version, 0);
        getDs().save(a);
        assertEquals(a.version, 1);
        final long version1 = a.version;

        getDs().save(a);
        assertEquals(a.version, 2);
        final long version2 = a.version;

        Assert.assertNotEquals(version1, version2);
    }

    @Entity
    private static class InvalidVersionUse {
        @Id
        private String id;
        @Version
        private long version1;
        @Version
        private long version2;

    }

    private static class NamedVersion extends TestEntity {
        @Version("v")
        private Long v;

        private String text;

        @Override
        public int hashCode() {
            return Objects.hash(v, text);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NamedVersion)) {
                return false;
            }
            final NamedVersion that = (NamedVersion) o;
            return Objects.equals(v, that.v) &&
                   Objects.equals(text, that.text);
        }
    }

    @Entity
    private static class VersionInHashcode {
        @Id
        private ObjectId id;
        @Version
        private Long version;

        private String data;

    }

    private static class VersionedType extends TestEntity {
        @Version
        private long version;
        private String text;
    }
}
