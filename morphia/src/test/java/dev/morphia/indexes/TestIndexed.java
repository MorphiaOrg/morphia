/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */

package dev.morphia.indexes;

import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import dev.morphia.annotations.Collation;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.NotSaved;
import dev.morphia.annotations.Property;
import dev.morphia.entities.IndexOnValue;
import dev.morphia.entities.NamedIndexOnValue;
import dev.morphia.entities.UniqueIndexOnValue;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappingException;
import dev.morphia.utils.IndexDirection;
import dev.morphia.utils.IndexType;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static dev.morphia.testutil.IndexMatcher.doesNotHaveIndexNamed;
import static dev.morphia.testutil.IndexMatcher.hasIndexNamed;

/**
 * @author Scott Hernandez
 */
public class TestIndexed extends TestBase {
    @Before
    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(UniqueIndexOnValue.class, IndexOnValue.class, NamedIndexOnValue.class);
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

    @Test
    public void shouldThrowExceptionWhenAddingADuplicateValueForAUniqueIndex() {
        getMorphia().map(UniqueIndexOnValue.class);
        getDs().ensureIndexes();
        long value = 7L;

        try {
            final UniqueIndexOnValue entityWithUniqueName = new UniqueIndexOnValue();
            entityWithUniqueName.setValue(value);
            entityWithUniqueName.setUnique(1);
            getDs().save(entityWithUniqueName);

            final UniqueIndexOnValue entityWithSameName = new UniqueIndexOnValue();
            entityWithSameName.setValue(value);
            entityWithSameName.setUnique(2);
            getDs().save(entityWithSameName);

            Assert.fail("Should have gotten a duplicate key exception");
        } catch (Exception ignored) {
        }

        value = 10L;
        try {
            final UniqueIndexOnValue first = new UniqueIndexOnValue();
            first.setValue(1);
            first.setUnique(value);
            getDs().save(first);

            final UniqueIndexOnValue second = new UniqueIndexOnValue();
            second.setValue(2);
            second.setUnique(value);
            getDs().save(second);

            Assert.fail("Should have gotten a duplicate key exception");
        } catch (Exception ignored) {
        }
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

    @Test
    public void testEmbeddedIndex() {
        final MappedClass mc = getMorphia().getMapper().addMappedClass(ContainsIndexedEmbed.class);

        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), doesNotHaveIndexNamed("e.name_-1"));
        getDs().ensureIndexes(ContainsIndexedEmbed.class);
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("e.name_-1"));
    }

    @Test
    public void testIndexedEntity() throws Exception {
        getDs().ensureIndexes();
        assertThat(getDs().getCollection(IndexOnValue.class).getIndexInfo(), hasIndexNamed("value_1"));

        getDs().save(new IndexOnValue());
        getDs().ensureIndexes();
        assertThat(getDs().getCollection(IndexOnValue.class).getIndexInfo(), hasIndexNamed("value_1"));
    }

    @Test
    public void testIndexedRecursiveEntity() throws Exception {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(CircularEmbeddedEntity.class);
        getDs().ensureIndexes();
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("a_1"));
    }

    @Test
    public void testIndexes() {
        final MappedClass mc = getMorphia().getMapper().addMappedClass(Ad2.class);

        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), doesNotHaveIndexNamed("active_1_lastMod_-1"));
        getDs().ensureIndexes(Ad2.class);
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("active_1_lastMod_-1"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testMultipleIndexedFields() {
        final MappedClass mc = getMorphia().getMapper().getMappedClass(Ad.class);
        getMorphia().map(Ad.class);

        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), doesNotHaveIndexNamed("lastMod_1_active_-1"));
        getDs().ensureIndex(Ad.class, "lastMod, -active");
        assertThat(getDb().getCollection(mc.getCollectionName()).getIndexInfo(), hasIndexNamed("lastMod_1_active_-1"));
    }

    @Test
    public void testNamedIndexEntity() throws Exception {
        getDs().ensureIndexes();

        assertThat(getDs().getCollection(NamedIndexOnValue.class).getIndexInfo(), hasIndexNamed("value_ascending"));
    }

    @Test(expected = DuplicateKeyException.class)
    public void testUniqueIndexedEntity() throws Exception {
        getDs().ensureIndexes();
        assertThat(getDs().getCollection(UniqueIndexOnValue.class).getIndexInfo(), hasIndexNamed("l_ascending"));
        getDs().save(new UniqueIndexOnValue("a"));

        // this should throw...
        getDs().save(new UniqueIndexOnValue("v"));
    }
    @Test(expected = MappingException.class)
    public void testMixedIndexDefinitions() throws Exception {
        getMorphia().map(MixedIndexDefinitions.class);
        getDs().ensureIndexes(MixedIndexDefinitions.class);
    }

    @Test
    public void testDefaults() {
        getMorphia().map(NewIndexed.class);
        getDs().ensureIndexes();
    }

    @Entity
    private static class NewIndexed {
        @Id
        ObjectId id;
        @Indexed(options = @IndexOptions(collation = @Collation(locale = "en_US")))
        String name;
    }

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


    @Entity
    private static class MixedIndexDefinitions {
        @Id
        private ObjectId id;
        @Indexed(unique = true, options = @IndexOptions(dropDups = true))
        private String name;
    }
}
