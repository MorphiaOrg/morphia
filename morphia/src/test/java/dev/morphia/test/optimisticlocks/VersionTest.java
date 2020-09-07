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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class VersionTest extends TestBase {
    @Test
    public void testConcurrentModification() {
        Assertions.assertThrows(ConcurrentModificationException.class, () -> {
            getMapper().map(List.of(VersionedType.class));

            final VersionedType a = new VersionedType();
            assertEquals(0, a.version);
            getDs().save(a);
            assertEquals(1, a.version);

            Query<VersionedType> query = getDs().find(VersionedType.class)
                                                .filter(eq("_id", a.getId()));
            getDs().save(query.iterator().next());

            assertEquals(2, query.iterator().next().version);

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

        assertEquals(initial.version, postUpdate.version);
    }

    @Test
    public void testInvalidVersionUse() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> getMapper().map(InvalidVersionUse.class));
    }

    @Test
    public void testManuallyIdentified() {
        Assertions.assertThrows(ConcurrentModificationException.class, () -> {

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
        Assertions.assertThrows(ConcurrentModificationException.class, () -> {
            final NamedVersion a = new NamedVersion();
            Assertions.assertNull(a.v);
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
            assertEquals(initial.get(i).id, type.id);
            assertEquals(initial.get(i).version + 1, type.version);
        }

        Assertions.assertThrows(ConcurrentModificationException.class, () -> {
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
        assertEquals(1, results.getModifiedCount());
        List<VersionedType> postUpdate = ds.find(VersionedType.class)
                                           .filter(eq("text", "some new value"))
                                           .iterator(new FindOptions()
                                                         .sort(Sort.ascending("_id")))
                                           .toList();

        for (int i = 0, postUpdateSize = postUpdate.size(); i < postUpdateSize; i++) {
            final VersionedType versionedType = postUpdate.get(i);
            assertEquals(initial.get(i).version + 1, versionedType.version);
        }
    }

    @Test
    public void testVersionFieldNameContribution() {
        final MappedField mappedFieldByJavaField = getMapper().getMappedClass(NamedVersion.class).getMappedFieldByJavaField("v");
        assertEquals("v", mappedFieldByJavaField.getMappedFieldName());
    }

    @Test
    public void testVersionInHashcode() {
        getMapper().mapPackage("com.example");

        final VersionInHashcode model = new VersionInHashcode();
        model.data = "whatever";
        getDs().save(model);
        Assertions.assertNotNull(model.version);
    }

    @Test
    public void testVersions() {
        final VersionedType a = new VersionedType();
        assertEquals(0, a.version);
        getDs().save(a);
        assertEquals(1, a.version);
        final long version1 = a.version;

        getDs().save(a);
        assertEquals(2, a.version);
        final long version2 = a.version;

        Assertions.assertNotEquals(version1, version2);
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
        public boolean equals(final Object o) {
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

    @Entity
    private static class VersionedType extends TestEntity {
        @Version
        private long version;
        private String text;
    }
}
