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

import com.mongodb.MongoWriteException;
import dev.morphia.TestBase;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.entities.IndexOnValue;
import dev.morphia.entities.NamedIndexOnValue;
import dev.morphia.entities.UniqueIndexOnValue;
import dev.morphia.utils.IndexDirection;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static dev.morphia.testutil.IndexMatcher.doesNotHaveIndexNamed;
import static dev.morphia.testutil.IndexMatcher.hasIndexNamed;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Scott Hernandez
 */
public class TestIndexed extends TestBase {
    @Before
    @Override
    public void setUp() {
        super.setUp();
        getMapper().map(UniqueIndexOnValue.class, IndexOnValue.class, NamedIndexOnValue.class);
    }

    @Test
    public void shouldThrowExceptionWhenAddingADuplicateValueForAUniqueIndex() {
        getMapper().map(UniqueIndexOnValue.class);
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
        getMapper().map(Place.class);

        // when
        getDs().ensureIndexes();

        // then
        List<Document> indexInfo = getIndexInfo(Place.class);
        assertThat(indexInfo.size(), is(2));
        assertThat(indexInfo, hasIndexNamed("location_2dsphere"));
    }

    @Test
    public void testCanCreate2dSphereIndexesOnLegacyCoordinatePairs() {
        // given
        getMapper().map(LegacyPlace.class);

        // when
        getDs().ensureIndexes();

        // then
        List<Document> indexInfo = getIndexInfo(LegacyPlace.class);
        assertThat(indexInfo, hasIndexNamed("location_2dsphere"));
    }

    @Test
    public void testIndexedEntity() {
        getDs().ensureIndexes();
        assertThat(getIndexInfo(IndexOnValue.class), hasIndexNamed("value_1"));

        getDs().save(new IndexOnValue());
        getDs().ensureIndexes();
        assertThat(getIndexInfo(IndexOnValue.class), hasIndexNamed("value_1"));
    }

    @Test
    public void testIndexedRecursiveEntity() {
        getMapper().getEntityModel(CircularEmbeddedEntity.class);
        getDs().ensureIndexes();
        assertThat(getIndexInfo(CircularEmbeddedEntity.class), hasIndexNamed("a_1"));
    }

    @Test
    public void testIndexes() {
        getMapper().getEntityModel(Ad2.class);

        assertThat(getIndexInfo(Ad2.class), doesNotHaveIndexNamed("active_1_lastMod_-1"));
        getDs().ensureIndexes(Ad2.class);
        assertThat(getIndexInfo(Ad2.class), hasIndexNamed("active_1_lastMod_-1"));
    }

    @Test
    public void testNamedIndexEntity() {
        getDs().ensureIndexes();

        assertThat(getIndexInfo(NamedIndexOnValue.class), hasIndexNamed("value_ascending"));
    }

    @Test(expected = MongoWriteException.class)
    public void testUniqueIndexedEntity() {
        getMapper().map(List.of(UniqueIndexOnValue.class));
        getDs().ensureIndexes();
        assertThat(getIndexInfo(UniqueIndexOnValue.class), hasIndexNamed("l_ascending"));
        getDs().save(new UniqueIndexOnValue("a"));

        // this should throw...
        getDs().save(new UniqueIndexOnValue("v"));
    }

    @Test
    public void testDefaults() {
        getMapper().map(List.of(NewIndexed.class));
        getDs().ensureIndexes();
    }

    @Entity
    private static class NewIndexed {
        @Id
        ObjectId id;
        @Indexed(options = @IndexOptions(collation = @Collation(locale = "en_US")))
        String name;
    }

    @Entity
    private static class Place {
        @Id
        private long id;

        @Indexed(IndexDirection.GEO2DSPHERE)
        private Object location;
    }

    @Entity
    private static class LegacyPlace {
        @Id
        private long id;

        @Indexed(IndexDirection.GEO2DSPHERE)
        private double[] location;
    }

    @Entity
    private static class Ad {
        @Id
        private long id;

        @Property("lastMod")
        @Indexed
        private long lastModified;

        @Indexed
        private boolean active;
    }

    @Entity
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

    @Entity
    private static class IndexedEmbed {
        @Indexed(IndexDirection.DESC)
        private String name;
    }

    @Entity
    private static class ContainsIndexedEmbed {
        @Id
        private ObjectId id;
        private IndexedEmbed e;
    }

    @Entity
    private static class CircularEmbeddedEntity {
        @Id
        private final ObjectId id = new ObjectId();
        private String name;
        @Indexed
        private CircularEmbeddedEntity a;
    }

}
