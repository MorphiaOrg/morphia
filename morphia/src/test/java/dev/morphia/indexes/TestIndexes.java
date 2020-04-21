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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.TestBase;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static dev.morphia.utils.IndexType.DESC;
import static org.junit.Assert.assertEquals;

public class TestIndexes extends TestBase {

    @Test
    public void testIndexes() {

        final Datastore datastore = getDs();
        datastore.find(TestWithIndexOption.class).delete(new DeleteOptions().multi(true));

        final MongoCollection<Document> indexOptionColl = getDatabase().getCollection(TestWithIndexOption.class.getSimpleName());
        indexOptionColl.drop();
        assertEquals(0, getIndexInfo(TestWithIndexOption.class).size());

        final MongoCollection<Document> depIndexColl = getDatabase().getCollection(TestWithDeprecatedIndex.class.getSimpleName());
        depIndexColl.drop();
        assertEquals(0, getIndexInfo(TestWithDeprecatedIndex.class).size());

        final MongoCollection<Document> hashIndexColl = getDatabase().getCollection(TestWithHashedIndex.class.getSimpleName());
        hashIndexColl.drop();
        assertEquals(0, getIndexInfo(TestWithHashedIndex.class).size());

        datastore.ensureIndexes(TestWithIndexOption.class);
        List<Document> indexInfo = getIndexInfo(TestWithIndexOption.class);
        assertEquals(2, indexInfo.size());
        assertBackground(indexInfo);
        for (Document document : indexInfo) {
            if (document.get("name").equals("collated")) {
                assertEquals(Document.parse("{ name : { $exists : true } }"),
                    document.get("partialFilterExpression"));
                Document collation = (Document) document.get("collation");
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

        datastore.ensureIndexes(TestWithDeprecatedIndex.class);
        assertEquals(2, getIndexInfo(TestWithDeprecatedIndex.class).size());
        assertBackground(getIndexInfo(TestWithDeprecatedIndex.class));

        datastore.ensureIndexes(TestWithHashedIndex.class);
        assertEquals(2, getIndexInfo(TestWithHashedIndex.class).size());
        assertHashed(getIndexInfo(TestWithHashedIndex.class));
    }

    private void assertBackground(final List<Document> indexInfo) {
        for (final Document document : indexInfo) {
            if (!document.getString("name").equals("_id_")) {
                Assert.assertTrue(document.getBoolean("background"));
            }
        }
    }

    private void assertHashed(final List<Document> indexInfo) {
        for (final Document document : indexInfo) {
            if (!document.getString("name").equals("_id_")) {
                assertEquals(((Document) document.get("key")).get("hashedValue"), "hashed");
            }
        }
    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(name = "collated",
        partialFilter = "{ name : { $exists : true } }",
        collation = @Collation(locale = "en_US", alternate = SHIFTED, backwards = true,
            caseFirst = CollationCaseFirst.UPPER, caseLevel = true, maxVariable = CollationMaxVariable.SPACE, normalization = true,
            numericOrdering = true, strength = CollationStrength.IDENTICAL),
        background = true),
        fields = {@Field(value = "name")})})
    public static class TestWithIndexOption {
        @Id
        private ObjectId id;
        private String name;

    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(background = true),
        fields = @Field("name"))})
    public static class TestWithDeprecatedIndex {
        @Id
        private ObjectId id;
        private String name;

    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(), fields = {@Field(value = "hashedValue", type = IndexType.HASHED)})})
    public static class TestWithHashedIndex {
        @Id
        private ObjectId id;
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
        private FeedEvent feedEvent;
    }
}
