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


package org.mongodb.morphia;


import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.annotations.CappedAt;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.utils.IndexDirection;
import org.mongodb.morphia.utils.IndexFieldDef;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Scott Hernandez
 */
public class TestIndexedCapped extends TestBase {
    @Entity(cap = @CappedAt(count = 1))
    private static class CurrentStatus {
        @Id
        private ObjectId id;
        private String message;

        private CurrentStatus() {
        }

        public CurrentStatus(final String msg) {
            message = msg;
        }
    }

    private static class IndexedClass {
        @Id
        private ObjectId id;
        @Indexed
        private long l = 4;
    }

    @Entity
    private static class NamedIndexClass {
        @Id
        private ObjectId id;
        @Indexed(name = "l_ascending")
        private long l = 4;
    }

    @Entity
    private static class UniqueIndexClass {
        @Id
        private ObjectId id;
        @Indexed(name = "l_ascending", unique = true)
        private long l = 4;
        private String name;

        UniqueIndexClass() {
        }

        UniqueIndexClass(final String name) {
            this.name = name;
        }
    }

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

    @Before
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(CurrentStatus.class).map(UniqueIndexClass.class).map(IndexedClass.class).map(NamedIndexClass.class);
    }

    @Test
    public void testCappedEntity() throws Exception {
        getDs().ensureCaps();
        final CurrentStatus cs = new CurrentStatus("All Good");
        getDs().save(cs);
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        getDs().save(new CurrentStatus("Kinda Bad"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        assertTrue(getDs().find(CurrentStatus.class).limit(1).get().message.contains("Bad"));
        getDs().save(new CurrentStatus("Kinda Bad2"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        getDs().save(new CurrentStatus("Kinda Bad3"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        getDs().save(new CurrentStatus("Kinda Bad4"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
    }

    @Test
    public void testIndexes() {
        final MappedClass mc = getMorphia().getMapper().addMappedClass(Ad2.class);

        assertFalse(hasNamedIndex("active_1_lastMod_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
        getDs().ensureIndexes(Ad2.class);
        assertTrue(hasNamedIndex("active_1_lastMod_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
    }

    @Test
    public void testEmbeddedIndex() {
        final MappedClass mc = getMorphia().getMapper().addMappedClass(ContainsIndexedEmbed.class);

        assertFalse(hasNamedIndex("e.name_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
        getDs().ensureIndexes(ContainsIndexedEmbed.class);
        assertTrue(hasNamedIndex("e.name_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
    }

    @Test
    public void testMultipleIndexedFields() {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(Ad.class);
        getMorphia().map(Ad.class);

        final IndexFieldDef[] definitions = {new IndexFieldDef("lastMod"), new IndexFieldDef("active", IndexDirection.DESC)};
        assertFalse(hasNamedIndex("lastMod_1_active_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
        getDs().ensureIndex(Ad.class, definitions);
        assertTrue(hasNamedIndex("lastMod_1_active_-1", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
    }

    @Test
    public void testIndexedRecursiveEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(CircularEmbeddedEntity.class);
        getDs().ensureIndexes();
        assertTrue(hasNamedIndex("a_1", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
    }

    @Test
    public void testIndexedEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(IndexedClass.class);
        getDs().ensureIndexes();
        assertTrue(hasIndexedField("l", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
        getDs().save(new IndexedClass());
        getDs().ensureIndexes();
        assertTrue(hasIndexedField("l", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
    }

    @Test
    public void testUniqueIndexedEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(UniqueIndexClass.class);
        getDs().ensureIndexes();
        assertTrue(hasIndexedField("l", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
        getDs().save(new UniqueIndexClass("a"));

        try {
            // this should throw...
            getDs().save(new UniqueIndexClass("v"));
            assertTrue(false);
            // } catch (MappingException me) {}
        } catch (Throwable me) {
            // currently is masked by java.lang.RuntimeException: json can't
        } 
        // serialize type : class com.mongodb.DBTimestamp

        getDs().ensureIndexes();
        assertTrue(hasIndexedField("l", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
    }

    @Test
    public void testNamedIndexEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(NamedIndexClass.class);
        getDs().ensureIndexes();
        assertTrue(hasIndexedField("l", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
        getDs().save(new IndexedClass());
        getDs().ensureIndexes();
        assertTrue(hasIndexedField("l", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));

        assertTrue(hasNamedIndex("l_ascending", getDb().getCollection(mc.getCollectionName()).getIndexInfo()));
    }

    protected boolean hasNamedIndex(final String name, final List<DBObject> indexes) {
        for (final DBObject dbObj : indexes) {
            if (dbObj.get("name").equals(name)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasIndexedField(final String name, final List<DBObject> indexes) {
        for (final DBObject dbObj : indexes) {
            if (((DBObject) dbObj.get("key")).containsField(name)) {
                return true;
            }
        }
        return false;
    }
}
