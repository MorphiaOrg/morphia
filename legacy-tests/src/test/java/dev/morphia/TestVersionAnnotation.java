package dev.morphia;

import com.mongodb.client.model.ReturnDocument;
import dev.morphia.entities.version.AbstractVersionedBase;
import dev.morphia.entities.version.Versioned;
import dev.morphia.entities.version.VersionedChildEntity;
import dev.morphia.mapping.MappedClass;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestVersionAnnotation extends TestBase {

    @Test
    public void testBulkUpdate() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        datastore.save(entity);

        entity = datastore.find(Versioned.class).filter(eq("_id", entity.getId())).first();
        Assert.assertEquals("Value 1", entity.getName());
        Assert.assertEquals(1, entity.getVersion().longValue());

        entity.setName("Value 2");
        datastore.save(entity);

        entity = datastore.find(Versioned.class).filter(eq("_id", entity.getId())).first();
        Assert.assertEquals("Value 2", entity.getName());
        Assert.assertEquals(2, entity.getVersion().longValue());

        Query<Versioned> query = datastore.find(Versioned.class);
        query.filter(eq("id", entity.getId()));
        query.update(set("name", "Value 3"))
             .execute();

        entity = datastore.find(Versioned.class).filter(eq("_id", entity.getId())).first();
        Assert.assertEquals("Value 3", entity.getName());
        Assert.assertEquals(3, entity.getVersion().longValue());
    }

    @Test
    public void testCanMapAPackageContainingAVersionedAbstractBaseClass() {
        getMapper().mapPackage("dev.morphia.entities.version");

        Collection<MappedClass> mappedClasses = getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(3));
        List<Class<?>> list = new ArrayList<>();
        for (MappedClass mappedClass : mappedClasses) {
            list.add(mappedClass.getType());
        }
        assertTrue(list.contains(VersionedChildEntity.class));
        assertTrue(list.contains(AbstractVersionedBase.class));
    }

    @Test
    public void testCanMapAnEntityWithAnAbstractVersionedParent() {
        getMapper().map(VersionedChildEntity.class);

        Collection<MappedClass> mappedClasses = getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(2));
        List<Class<?>> list = new ArrayList<>();
        for (MappedClass mappedClass : mappedClasses) {
            list.add(mappedClass.getType());
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

        Assert.assertEquals("Value 3", entity.getName());
        Assert.assertEquals(1, entity.getVersion().longValue());
        Assert.assertNotNull(entity.getId());
    }

    @Test
    public void testIncVersionNotOverridingOtherInc() {
        final Versioned version1 = new Versioned();
        version1.setCount(0);
        getDs().save(version1);

        assertEquals(Long.valueOf(1), version1.getVersion());
        assertEquals(0, version1.getCount());

        Query<Versioned> query = getDs().find(Versioned.class);
        query.filter(eq("_id", version1.getId()));
        query.update(inc("count"))
             .execute(new UpdateOptions().upsert(true));

        final Versioned version2 = getDs().find(Versioned.class)
                                          .filter(eq("_id", version1.getId()))
                                          .first();

        assertEquals(Long.valueOf(2), version2.getVersion());
        assertEquals(1, version2.getCount());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testThrowsExceptionWhenTryingToSaveAnOldVersion() {
        // given
        final Versioned version1 = new Versioned();
        getDs().save(version1);
        final Versioned version2 = getDs().find(Versioned.class).filter(eq("_id", version1.getId())).first();
        getDs().save(version2);

        // when
        getDs().save(version1);
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
    public void testVersionedInserts() {
        List<Versioned> list = asList(new Versioned(), new Versioned(), new Versioned(), new Versioned(), new Versioned());
        getDs().insert(list);
        for (Versioned versioned : list) {
            assertNotNull(versioned.getVersion());
        }
    }

    @Test
    public void testVersionNumbersIncrementWithEachSave() {
        final Versioned version1 = new Versioned();
        getDs().save(version1);
        assertEquals(Long.valueOf(1), version1.getVersion());

        final Versioned version2 = getDs().find(Versioned.class).filter(eq("_id", version1.getId())).first();
        getDs().save(version2);
        assertEquals(Long.valueOf(2), version2.getVersion());
    }

    @Test
    public void testVersionedUpsert() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        Query<Versioned> query = datastore.find(Versioned.class);
        query.filter(eq("name", "Value 1"));
        query.update(set("name", "Value 3"))
             .execute(new UpdateOptions().upsert(true));

        entity = datastore.find(Versioned.class).iterator(new FindOptions().limit(1)).tryNext();
        Assert.assertEquals("Value 3", entity.getName());
        Assert.assertEquals(1, entity.getVersion().longValue());
    }
}
