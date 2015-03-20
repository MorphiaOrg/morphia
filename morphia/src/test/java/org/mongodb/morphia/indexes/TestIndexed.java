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
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.entities.IndexOnValue;
import org.mongodb.morphia.entities.NamedIndexOnValue;
import org.mongodb.morphia.entities.UniqueIndexOnValue;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.utils.IndexDirection;
import org.mongodb.morphia.utils.IndexType;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mongodb.morphia.testutil.IndexMatcher.doesNotHaveIndexNamed;
import static org.mongodb.morphia.testutil.IndexMatcher.hasIndexNamed;

/**
 * @author Scott Hernandez
 */
public class TestIndexed extends TestBase {
    @SuppressWarnings("unused")
    private static class Place {
        @Id
        private long id;

        @Indexed(IndexDirection.GEO2DSPHERE)
        private Object location;
    }

    @SuppressWarnings("unused")
    private static class LegacyPlace {
        @Id
        private long id;

        @Indexed(IndexDirection.GEO2DSPHERE)
        private double[] location;
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

    @Indexes(@Index(fields = {@Field("active"), @Field(value = "lastModified", type = IndexType.DESC)},
                       options = @IndexOptions(unique = true)))
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

        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), doesNotHaveIndexNamed("active_1_lastMod_-1"));
        getDs().ensureIndexes(Ad2.class);
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("active_1_lastMod_-1"));
    }

    @Test
    public void testEmbeddedIndex() {
        final MappedClass mc = getMorphia().getMapper().addMappedClass(ContainsIndexedEmbed.class);

        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), doesNotHaveIndexNamed("e.name_-1"));
        getDs().ensureIndexes(ContainsIndexedEmbed.class);
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("e.name_-1"));
    }

    @Test
    public void testMultipleIndexedFields() {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(Ad.class);
        getMorphia().map(Ad.class);

        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), doesNotHaveIndexNamed("lastMod_1_active_-1"));
        getDs().ensureIndex(Ad.class, "lastMod, -active");
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("lastMod_1_active_-1"));
    }

    @Test
    public void testIndexedRecursiveEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(CircularEmbeddedEntity.class);
        getDs().ensureIndexes();
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("a_1"));
    }

    @Test
    public void testIndexedEntity() throws Exception {
        getDs().ensureIndexes();
        assertThat(getDs().getCollection(IndexOnValue.class).getIndexInfo(), hasIndexNamed("value_1"));

        getDs().save(new IndexOnValue());
        getDs().ensureIndexes();
        assertThat(getDs().getCollection(IndexOnValue.class).getIndexInfo(), hasIndexNamed("value_1"));
    }

    @Test(expected = DuplicateKeyException.class)
    public void testUniqueIndexedEntity() throws Exception {
        getDs().ensureIndexes();
        assertThat(getDs().getCollection(UniqueIndexOnValue.class).getIndexInfo(), hasIndexNamed("l_ascending"));
        getDs().save(new UniqueIndexOnValue("a"));

        // this should throw...
        getDs().save(new UniqueIndexOnValue("v"));
    }

    @Test
    public void testNamedIndexEntity() throws Exception {
        getDs().ensureIndexes();

        assertThat(getDs().getCollection(NamedIndexOnValue.class).getIndexInfo(), hasIndexNamed("value_ascending"));
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

    @Test
    public void testCanCreate2dSphereIndexes() {
        // given
        getMorphia().map(Place.class);

        // when
        getDs().ensureIndexes();

        // then
        List<DBObject> indexInfo = getDs().getCollection(Place.class).getIndexInfo();
        assertThat(indexInfo.size(), is(2));
        assertThat(indexInfo, hasIndexNamed("location_2dsphere"));
    }

    @Test
    public void testCanCreate2dSphereIndexesOnLegacyCoordinatePairs() {
        // given
        getMorphia().map(LegacyPlace.class);

        // when
        getDs().ensureIndexes();

        // then
        List<DBObject> indexInfo = getDs().getCollection(LegacyPlace.class).getIndexInfo();
        assertThat(indexInfo, hasIndexNamed("location_2dsphere"));
    }

}
