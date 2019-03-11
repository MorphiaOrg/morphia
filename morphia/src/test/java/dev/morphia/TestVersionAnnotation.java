/*
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package dev.morphia;

import org.junit.Assert;
import org.junit.Test;
import dev.morphia.entities.version.AbstractVersionedBase;
import dev.morphia.entities.version.Versioned;
import dev.morphia.entities.version.VersionedChildEntity;
import dev.morphia.mapping.MappedClass;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestVersionAnnotation extends TestBase {

    @Test
    public void testBulkUpdate() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        datastore.save(entity);

        entity = datastore.get(Versioned.class, entity.getId());
        Assert.assertEquals("Value 1", entity.getName());
        Assert.assertEquals(1, entity.getVersion().longValue());

        entity.setName("Value 2");
        datastore.save(entity);

        entity = datastore.get(Versioned.class, entity.getId());
        Assert.assertEquals("Value 2", entity.getName());
        Assert.assertEquals(2, entity.getVersion().longValue());

        Query<Versioned> query = datastore.find(Versioned.class);
        query.filter("id", entity.getId());
        UpdateOperations<Versioned> ops = datastore.createUpdateOperations(Versioned.class);
        ops.set("name", "Value 3");
        datastore.update(query, ops);

        entity = datastore.get(Versioned.class, entity.getId());
        Assert.assertEquals("Value 3", entity.getName());
        Assert.assertEquals(3, entity.getVersion().longValue());
    }

    @Test
    public void testCanMapAPackageContainingAVersionedAbstractBaseClass() {
        Morphia morphia = getMorphia().mapPackage("dev.morphia.entities.version");

        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(2));
        List<Class<?>> list = new ArrayList<Class<?>>();
        for (MappedClass mappedClass : mappedClasses) {
            list.add(mappedClass.getClazz());
        }
        assertTrue(list.contains(VersionedChildEntity.class));
        assertTrue(list.contains(AbstractVersionedBase.class));
    }

    @Test
    public void testCanMapAnEntityWithAnAbstractVersionedParent() {
        Morphia morphia = getMorphia().map(VersionedChildEntity.class);

        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(2));
        List<Class<?>> list = new ArrayList<Class<?>>();
        for (MappedClass mappedClass : mappedClasses) {
            list.add(mappedClass.getClazz());
        }
        assertTrue(list.contains(VersionedChildEntity.class));
        assertTrue(list.contains(AbstractVersionedBase.class));
    }

    @Test
    public void testEntityUpdate() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        datastore.save(entity);

        entity = datastore.get(Versioned.class, entity.getId());
        Assert.assertEquals("Value 1", entity.getName());
        Assert.assertEquals(1, entity.getVersion().longValue());

        entity.setName("Value 2");
        datastore.save(entity);

        entity = datastore.get(Versioned.class, entity.getId());
        Assert.assertEquals("Value 2", entity.getName());
        Assert.assertEquals(2, entity.getVersion().longValue());

        UpdateOperations<Versioned> ops = datastore.createUpdateOperations(Versioned.class);
        ops.set("name", "Value 3");
        Assert.assertEquals(1, datastore.update(entity, ops).getUpdatedCount());
        Assert.assertEquals(0, datastore.update(entity, ops).getUpdatedCount());

        entity = datastore.get(Versioned.class, entity.getId());
        Assert.assertEquals("Value 3", entity.getName());
        Assert.assertEquals(3, entity.getVersion().longValue());

        ops = datastore.createUpdateOperations(Versioned.class);
        ops.set("name", "Value 4");
        datastore.update(datastore.getKey(entity), ops);

        entity = datastore.get(Versioned.class, entity.getId());
        Assert.assertEquals("Value 4", entity.getName());
        Assert.assertEquals(4, entity.getVersion().longValue());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateFirst() {
        final Datastore datastore = getDs();

        Versioned original = new Versioned();
        original.setName("Value 1");
        original.setCount(42);
        getDs().save(original);

        Versioned update = new Versioned();
        update.setName("Value 2");

        Query<Versioned> query = datastore.find(Versioned.class).field("name").equal("Value 1");
        try {
            datastore.updateFirst(
                query,
                update, true);
            fail("This call should have been rejected");
        } catch (UnsupportedOperationException ignored) {
        }

        datastore.updateFirst(
            query,
            datastore.createUpdateOperations(Versioned.class).inc("count"), true);
        assertEquals(43, query.find(new FindOptions().limit(1)).tryNext().getCount());
    }

    @Test
    public void testIncVersionNotOverridingOtherInc() {
        final Versioned version1 = new Versioned();
        version1.setCount(0);
        getDs().save(version1);

        assertEquals(new Long(1), version1.getVersion());
        assertEquals(0, version1.getCount());

        Query<Versioned> query = getDs().find(Versioned.class);
        query.field("_id").equal(version1.getId());
        UpdateOperations<Versioned> up = getDs().createUpdateOperations(Versioned.class).inc("count");

        getDs().update(query, up, new UpdateOptions().upsert(true));

        final Versioned version2 = getDs().get(Versioned.class, version1.getId());

        assertEquals(new Long(2), version2.getVersion());
        assertEquals(1, version2.getCount());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testThrowsExceptionWhenTryingToSaveAnOldVersion() throws Exception {
        // given
        final Versioned version1 = new Versioned();
        getDs().save(version1);
        final Versioned version2 = getDs().get(Versioned.class, version1.getId());
        getDs().save(version2);

        // when
        getDs().save(version1);
    }

    @Test
    public void testUpdatesToVersionedFileAreReflectedInTheDatastore() {
        final Versioned version1 = new Versioned();
        version1.setName("foo");

        this.getDs().save(version1);

        final Versioned version1Updated = getDs().get(Versioned.class, version1.getId());
        version1Updated.setName("bar");

        this.getDs().merge(version1Updated);

        final Versioned versionedEntityFromDs = this.getDs().get(Versioned.class, version1.getId());
        assertEquals(version1Updated.getName(), versionedEntityFromDs.getName());
    }

    @Test
    public void testVersionNumbersIncrementWithEachSave() throws Exception {
        final Versioned version1 = new Versioned();
        getDs().save(version1);
        assertEquals(new Long(1), version1.getVersion());

        final Versioned version2 = getDs().get(Versioned.class, version1.getId());
        getDs().save(version2);
        assertEquals(new Long(2), version2.getVersion());
    }

    @Test
    public void testVersionedInserts() {
        List<Versioned> list = asList(new Versioned(), new Versioned(), new Versioned(), new Versioned(), new Versioned());
        getAds().insert(list);
        for (Versioned versioned : list) {
            assertNotNull(versioned.getVersion());
        }
    }

    @Test
    public void testVersionedUpsert() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        Query<Versioned> query = datastore.find(Versioned.class);
        query.filter("name", "Value 1");
        UpdateOperations<Versioned> ops = datastore.createUpdateOperations(Versioned.class);
        ops.set("name", "Value 3");
        datastore.update(query, ops, new UpdateOptions().upsert(true));

        entity = datastore.find(Versioned.class).find(new FindOptions().limit(1)).tryNext();
        Assert.assertEquals("Value 3", entity.getName());
        Assert.assertEquals(1, entity.getVersion().longValue());
    }

}
