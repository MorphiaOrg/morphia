/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.indexes;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.Builder;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static dev.morphia.utils.IndexType.DESC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestIndexes extends TestBase {

    @Test
    public void testIndexes() {

        final Datastore datastore = getDs();
        datastore.delete(datastore.find(TestWithIndexOption.class));

        final DBCollection indexOptionColl = getDs().getCollection(TestWithIndexOption.class);
        indexOptionColl.drop();
        assertEquals(0, indexOptionColl.getIndexInfo().size());

        final DBCollection depIndexColl = getDs().getCollection(TestWithDeprecatedIndex.class);
        depIndexColl.drop();
        assertEquals(0, depIndexColl.getIndexInfo().size());

        final DBCollection hashIndexColl = getDs().getCollection(TestWithHashedIndex.class);
        hashIndexColl.drop();
        assertEquals(0, hashIndexColl.getIndexInfo().size());

        if (serverIsAtLeastVersion(3.4)) {
            datastore.ensureIndexes(TestWithIndexOption.class, true);
            assertEquals(2, indexOptionColl.getIndexInfo().size());
            List<DBObject> indexInfo = indexOptionColl.getIndexInfo();
            assertBackground(indexInfo);
            for (DBObject dbObject : indexInfo) {
                if (dbObject.get("name").equals("collated")) {
                    assertEquals(BasicDBObject.parse("{ name : { $exists : true } }"),
                        dbObject.get("partialFilterExpression"));
                    BasicDBObject collation = (BasicDBObject) dbObject.get("collation");
                    assertEquals("en_US", collation.get("locale"));
                    assertEquals("upper", collation.get("caseFirst"));
                    assertEquals("shifted", collation.get("alternate"));
                    Assert.assertTrue(collation.getBoolean("backwards"));
                    assertEquals("upper", collation.get("caseFirst"));
                    Assert.assertTrue(collation.getBoolean("caseLevel"));
                    assertEquals("space", collation.get("maxVariable"));
                    Assert.assertTrue(collation.getBoolean("normalization"));
                    Assert.assertTrue(collation.getBoolean("numericOrdering"));
                    assertEquals(5, collation.get("strength"));
                }
            }
        }

        datastore.ensureIndexes(TestWithDeprecatedIndex.class, true);
        assertEquals(2, depIndexColl.getIndexInfo().size());
        assertBackground(depIndexColl.getIndexInfo());

        datastore.ensureIndexes(TestWithHashedIndex.class);
        assertEquals(2, hashIndexColl.getIndexInfo().size());
        assertHashed(hashIndexColl.getIndexInfo());
    }

    @Test
    public void embeddedIndexPartialFilters() {
        getMorphia().map(FeedEvent.class, InboxEvent.class);
        getDs().ensureIndexes();
        final MongoCollection<Document> inboxEvent = getDatabase().getCollection("InboxEvent");
        for (final Document index : inboxEvent.listIndexes()) {
            if (!"_id_".equals(index.get("name"))) {
                for (String name : index.get("key", Document.class).keySet()) {
                    Assert.assertTrue("Key names should start with the field name: " + name, name.startsWith("feedEvent."));
                }
            }
        }

        // check the logging is disabled
        inboxEvent.drop();
        final Builder builder = MapperOptions
                                    .builder(getMorphia().getMapper().getOptions())
                                    .disableEmbeddedIndexes(true);
        getMorphia().getMapper().setOptions(builder.build());
        getDs().ensureIndexes();
        Assert.assertNull("No indexes should be generated for InboxEvent", inboxEvent.listIndexes().iterator().tryNext());
    }

    @Test
    public void optionsFromOldSyntax() {
        getMorphia().map(MongoSettingsHistory.class);
        getDs().ensureIndex(MongoSettingsHistory.class, "query_by_key_and_date", "key, newValue, -date", true, false);

        final List<DBObject> indexes = getDs().getCollection(MongoSettingsHistory.class).getIndexInfo();
        DBObject index = null;
        for (final DBObject possible : indexes) {
            if("query_by_key_and_date".equals(possible.get("name"))) {
                index = possible;
            }
        }
        Assert.assertNotNull(index);
        assertTrue((Boolean) index.get("unique"));
    }

    @Entity
    private static class MongoSettingsHistory {
        @Id
        ObjectId id;
        String key;
        Date date;
        Integer newValue;
    }

    private void assertBackground(final List<DBObject> indexInfo) {
        for (final DBObject dbObject : indexInfo) {
            BasicDBObject index = (BasicDBObject) dbObject;
            if (!index.getString("name").equals("_id_")) {
                Assert.assertTrue(index.getBoolean("background"));
            }
        }
    }

    private void assertHashed(final List<DBObject> indexInfo) {
        for (final DBObject dbObject : indexInfo) {
            BasicDBObject index = (BasicDBObject) dbObject;
            if (!index.getString("name").equals("_id_")) {
                assertEquals(((DBObject) index.get("key")).get("hashedValue"), "hashed");
            }
        }
    }

    @Entity(noClassnameStored = true)
    @Indexes({@Index(options = @IndexOptions(name = "collated",
        partialFilter = "{ name : { $exists : true } }",
        collation = @Collation(locale = "en_US", alternate = SHIFTED, backwards = true,
            caseFirst = CollationCaseFirst.UPPER, caseLevel = true, maxVariable = CollationMaxVariable.SPACE, normalization = true,
            numericOrdering = true, strength = CollationStrength.IDENTICAL)),
        fields = {@Field(value = "name")})})
    public static class TestWithIndexOption {
        private String name;

    }

    @Entity(noClassnameStored = true)
    @Indexes({@Index("name")})
    public static class TestWithDeprecatedIndex {


        private String name;

    }

    @Entity(noClassnameStored = true)
    @Indexes({@Index(options = @IndexOptions(), fields = {@Field(value = "hashedValue", type = IndexType.HASHED)})})
    public static class TestWithHashedIndex {
        private String hashedValue;

    }

    @Entity
    @Indexes(@Index(fields = {@Field("actor.actorObject.userId"), @Field(value = "actor.actorType", type = DESC)},
        options = @IndexOptions(disableValidation = true,
            partialFilter = "{ 'actor.actorObject.userId': { $exists: true }, 'actor.actorType': { $exists: true } }")))
    public static class FeedEvent {
        @Id
        private ObjectId id;
    }

    @Entity
    public static class InboxEvent {
        @Id
        private ObjectId id;
        @Embedded
        private FeedEvent feedEvent;
    }
}
