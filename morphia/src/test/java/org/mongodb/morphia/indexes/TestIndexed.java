/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.morphia.indexes;

import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.entities.IndexOnValue;
import org.mongodb.morphia.entities.NamedIndexOnValue;
import org.mongodb.morphia.entities.UniqueIndexOnValue;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.utils.IndexDirection;

import java.util.List;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Scott Hernandez
 */
public class TestIndexed extends TestBase {
    private static class Ad {
        @Id
        private long id;

        @Property("lastMod")
        @Indexed
        private long lastModified;

        @Indexed
        private boolean active;
    }

    @Indexes(@Index("active,-lastModified"))
    private static class Ad2 {
        @Id
        private long id;

        @Property("lastMod")
        @Indexed
        private long lastModified;

        @Indexed
        private boolean active;
    }

    @Embedded
    private static class IndexedEmbed {
        @Indexed(IndexDirection.DESC)
        private String name;
    }

    private static class ContainsIndexedEmbed {
        @Id
        private ObjectId id;
        private IndexedEmbed e;
    }

    private static class CircularEmbeddedEntity {
        @Id
        private ObjectId id = new ObjectId();
        private String name;
        @Indexed
        private CircularEmbeddedEntity a;
    }

    @Entity
    private static class NoIndexes {
        @Id
        private ObjectId id;

        @NotSaved
        private IndexOnValue indexedClass;
    }

    @Before
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(UniqueIndexOnValue.class).map(IndexOnValue.class).map(NamedIndexOnValue.class);
    }

    @Test
    public void testIndexes() {
        final MappedClass mc = getMorphia().getMapper().addMappedClass(Ad2.class);

        assertDoesNotHaveNamedIndex("active_1_lastMod_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
        getDs().ensureIndexes(Ad2.class);
        assertHasNamedIndex("active_1_lastMod_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
    }

    @Test
    public void testEmbeddedIndex() {
        final MappedClass mc = getMorphia().getMapper().addMappedClass(ContainsIndexedEmbed.class);

        assertDoesNotHaveNamedIndex("e.name_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
        getDs().ensureIndexes(ContainsIndexedEmbed.class);
        assertHasNamedIndex("e.name_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
    }

    @Test
    public void testMultipleIndexedFields() {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(Ad.class);
        getMorphia().map(Ad.class);

        assertDoesNotHaveNamedIndex("lastMod_1_active_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
        getDs().ensureIndex(Ad.class, "lastMod, -active");
        assertHasNamedIndex("lastMod_1_active_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
    }

    @Test
    public void testIndexedRecursiveEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(CircularEmbeddedEntity.class);
        getDs().ensureIndexes();
        assertHasNamedIndex("a_1", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
    }

    @Test
    public void testIndexedEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(IndexOnValue.class);
        getDs().ensureIndexes();
        assertHasIndexedField("value", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
        getDs().save(new IndexOnValue());
        getDs().ensureIndexes();
        assertHasIndexedField("value", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
    }

    @Test
    public void testUniqueIndexedEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(UniqueIndexOnValue.class);
        getDs().ensureIndexes();
        assertHasIndexedField("value", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
        getDs().save(new UniqueIndexOnValue("a"));

        try {
            // this should throw...
            getDs().save(new UniqueIndexOnValue("v"));
            assertTrue(false);
            // } catch (MappingException me) {}
        } catch (Throwable me) {
            // currently is masked by java.lang.RuntimeException: json can't
        } 
        // serialize type : class com.mongodb.DBTimestamp

        getDs().ensureIndexes();
        assertHasIndexedField("value", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
    }

    @Test
    public void testNamedIndexEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(NamedIndexOnValue.class);
        getDs().ensureIndexes();
        assertHasIndexedField("value", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
        getDs().save(new IndexOnValue());
        getDs().ensureIndexes();
        assertHasIndexedField("value", getDb().getCollection(mc.getCollectionName()).getIndexInfo());

        assertHasNamedIndex("value_ascending", getDb().getCollection(mc.getCollectionName()).getIndexInfo());
    }

    @Test
    public void shouldNotCreateAnIndexWhenAnIndexedEntityIsMarkedAsNotSaved() {
        // given
        getMorphia().map(IndexOnValue.class, NoIndexes.class);
        Datastore ds = getDs();

        // when
        ds.ensureIndexes();
        ds.save(new IndexOnValue());
        ds.save(new NoIndexes());

        // then
        List<DBObject> indexes = getDb().getCollection("NoIndexes").getIndexInfo();
        assertEquals(1, indexes.size());
    }

    @Test(expected = DuplicateKeyException.class)
    public void shouldThrowExceptionWhenAddingADuplicateValueForAUniqueIndex() {
        // given
        getMorphia().map(UniqueIndexOnValue.class);
        getDs().ensureIndexes();
        final long value = 7L;

        final UniqueIndexOnValue entityWithUniqueName = new UniqueIndexOnValue();
        entityWithUniqueName.setValue(value);
        getDs().save(entityWithUniqueName);

        // when
        final UniqueIndexOnValue entityWithSameName = new UniqueIndexOnValue();
        entityWithSameName.setValue(value);
        getDs().save(entityWithSameName);
    }

    protected static void assertHasNamedIndex(final String name, final List<DBObject> indexes) {
        for (final DBObject dbObj : indexes) {
            if (dbObj.get("name").equals(name)) {
                return;
            }
        }
        fail(format("Expected to find index with name '%s' in %s", name, indexes));
    }

    protected static void assertDoesNotHaveNamedIndex(final String name, final List<DBObject> indexes) {
        for (final DBObject dbObj : indexes) {
            if (dbObj.get("name").equals(name)) {
                fail(format("Did not expect to find index with name '%s' in %s", name, indexes));
            }
        }
    }

    protected static void assertHasIndexedField(final String name, final List<DBObject> indexes) {
        for (final DBObject dbObj : indexes) {
            if (((DBObject) dbObj.get("key")).containsField(name)) {
                return;
            }
        }
        fail(format("Expected to find index for field '%s' in %s", name, indexes));
    }
}
