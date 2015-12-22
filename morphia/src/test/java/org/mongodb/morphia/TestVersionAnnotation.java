/**
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

package org.mongodb.morphia;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.entities.version.Versioned;
import org.mongodb.morphia.entities.version.VersionedChildEntity;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.Collection;
import java.util.ConcurrentModificationException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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

        Query<Versioned> query = datastore.createQuery(Versioned.class);
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
        // when
        Morphia morphia = getMorphia().mapPackage("org.mongodb.morphia.entities.version");

        // then
        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(1));
        assertEquals(mappedClasses.iterator().next().getClazz(), VersionedChildEntity.class);
    }

    @Test
    public void testCanMapAnEntityWithAnAbstractVersionedParent() {
        // when
        Morphia morphia = getMorphia().map(VersionedChildEntity.class);

        // then
        Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
        assertThat(mappedClasses.size(), is(1));
        assertEquals(mappedClasses.iterator().next().getClazz(), VersionedChildEntity.class);
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
        datastore.update(entity, ops);

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
    public void testIncVersionNotOverridingOtherInc() {
        final Versioned version1 = new Versioned();
        version1.setCount(0);
        getDs().save(version1);

        assertEquals(new Long(1), version1.getVersion());
        assertEquals(0, version1.getCount());

        Query<Versioned> query = getDs().createQuery(Versioned.class);
        query.field("_id").equal(version1.getId());
        UpdateOperations<Versioned> up = getDs().createUpdateOperations(Versioned.class).inc("count");

        getDs().updateFirst(query, up, true);

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
        Versioned[] versioneds = {new Versioned(), new Versioned(), new Versioned(), new Versioned(), new Versioned()};
        getAds().insert(versioneds);
        for (Versioned versioned : versioneds) {
            assertNotNull(versioned.getVersion());
        }
    }

    @Test
    public void testVersionedUpsert() {
        final Datastore datastore = getDs();

        Versioned entity = new Versioned();
        entity.setName("Value 1");

        Query<Versioned> query = datastore.createQuery(Versioned.class);
        query.filter("name", "Value 1");
        UpdateOperations<Versioned> ops = datastore.createUpdateOperations(Versioned.class);
        ops.set("name", "Value 3");
        datastore.update(query, ops, true);

        entity = datastore.createQuery(Versioned.class).get();
        Assert.assertEquals("Value 3", entity.getName());
        Assert.assertEquals(1, entity.getVersion().longValue());
    }

}
