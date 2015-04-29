package org.mongodb.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import java.io.Serializable;


public class CompoundIdTest extends TestBase {

    @Embedded
    private static class CompoundId implements Serializable {
        private final ObjectId id = new ObjectId();
        private String name;

        CompoundId() {
        }

        CompoundId(final String n) {
            name = n;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof CompoundId)) {
                return false;
            }
            final CompoundId other = ((CompoundId) obj);
            return other.id.equals(id) && other.name.equals(name);
        }

    }

    private static class CompoundIdEntity {
        @Id
        private CompoundId id;
        private String e;
        @Reference
        private CompoundIdEntity sibling;

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CompoundIdEntity that = (CompoundIdEntity) o;

            if (!id.equals(that.id)) {
                return false;
            }
            if (e != null ? !e.equals(that.e) : that.e != null) {
                return false;
            }
            return !(sibling != null ? !sibling.equals(that.sibling) : that.sibling != null);

        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (e != null ? e.hashCode() : 0);
            result = 31 * result + (sibling != null ? sibling.hashCode() : 0);
            return result;
        }
    }


    @Test
    public void testMapping() throws Exception {
        CompoundIdEntity entity = new CompoundIdEntity();
        entity.id = new CompoundId("test");

        getDs().save(entity);
        entity = getDs().get(entity);
        Assert.assertEquals("test", entity.id.name);
        Assert.assertNotNull(entity.id.id);
    }

    @Test
    public void testDelete() throws Exception {
        final CompoundIdEntity entity = new CompoundIdEntity();
        entity.id = new CompoundId("test");

        getDs().save(entity);
        getDs().delete(CompoundIdEntity.class, entity.id);
    }

    @Test
    public void testOtherDelete() throws Exception {
        final CompoundIdEntity entity = new CompoundIdEntity();
        entity.id = new CompoundId("test");

        getDs().save(entity);
        ((AdvancedDatastore) getDs()).delete(getDs().getCollection(CompoundIdEntity.class).getName(), CompoundIdEntity.class, entity.id);
    }

    @Test
    @Ignore("https://github.com/mongodb/morphia/issues/675")
    public void testReference() {
        getMorphia().map(CompoundIdEntity.class, CompoundId.class);
        getDs().getCollection(CompoundIdEntity.class).drop();

        final CompoundIdEntity sibling = new CompoundIdEntity();
        sibling.id = new CompoundId("sibling ID");
        getDs().save(sibling);

        final CompoundIdEntity entity = new CompoundIdEntity();
        entity.id = new CompoundId("entity ID");
        entity.e = "some value";
        entity.sibling = sibling;
        getDs().save(entity);

        final CompoundIdEntity loaded = getDs().get(entity);
        Assert.assertEquals(entity, loaded);
    }
}
