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

package dev.morphia.test.indexes;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.methods.MethodMappedUser;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static dev.morphia.Morphia.createDatastore;
import static dev.morphia.utils.IndexType.DESC;

public class TestIndexes extends TestBase {

    @Test
    public void testIndexes() {

        final Datastore datastore = getDs();
        datastore.find(TestWithIndexOption.class).delete(new DeleteOptions().multi(true));

        final MongoCollection<Document> indexOptionColl = getDatabase().getCollection(TestWithIndexOption.class.getSimpleName());
        indexOptionColl.drop();
        Assert.assertEquals(getIndexInfo(TestWithIndexOption.class).size(), 0);

        final MongoCollection<Document> depIndexColl = getDatabase().getCollection(TestWithDeprecatedIndex.class.getSimpleName());
        depIndexColl.drop();
        Assert.assertEquals(getIndexInfo(TestWithDeprecatedIndex.class).size(), 0);

        final MongoCollection<Document> hashIndexColl = getDatabase().getCollection(TestWithHashedIndex.class.getSimpleName());
        hashIndexColl.drop();
        Assert.assertEquals(getIndexInfo(TestWithHashedIndex.class).size(), 0);

        datastore.ensureIndexes(TestWithIndexOption.class);
        List<Document> indexInfo = getIndexInfo(TestWithIndexOption.class);
        Assert.assertEquals(indexInfo.size(), 2);
        assertBackground(indexInfo);
        for (Document document : indexInfo) {
            if (document.get("name").equals("collated")) {
                Assert.assertEquals(document.get("partialFilterExpression"),
                    Document.parse("{ name : { $exists : true } }"));
                Document collation = (Document) document.get("collation");
                Assert.assertEquals(collation.get("locale"), "en_US");
                Assert.assertEquals(collation.get("caseFirst"), "upper");
                Assert.assertEquals(collation.get("alternate"), "shifted");
                Assert.assertTrue(collation.getBoolean("backwards"));
                Assert.assertEquals(collation.get("caseFirst"), "upper");
                Assert.assertTrue(collation.getBoolean("caseLevel"));
                Assert.assertEquals(collation.get("maxVariable"), "space");
                Assert.assertTrue(collation.getBoolean("normalization"));
                Assert.assertTrue(collation.getBoolean("numericOrdering"));
                Assert.assertEquals(collation.get("strength"), 5);
            }
        }

        datastore.ensureIndexes(TestWithDeprecatedIndex.class);
        Assert.assertEquals(getIndexInfo(TestWithDeprecatedIndex.class).size(), 2);
        assertBackground(getIndexInfo(TestWithDeprecatedIndex.class));

        datastore.ensureIndexes(TestWithHashedIndex.class);
        Assert.assertEquals(getIndexInfo(TestWithHashedIndex.class).size(), 2);
        assertHashed(getIndexInfo(TestWithHashedIndex.class));
    }

    @Test
    public void testMethodMapping() {
        Datastore datastore = createDatastore(getMongoClient(), TEST_DB_NAME,
            MapperOptions.builder()
                         .propertyDiscovery(
                             PropertyDiscovery.METHODS)
                         .build());

        EntityModel model = datastore.getMapper().map(MethodMappedUser.class).get(0);
        datastore.ensureIndexes(MethodMappedUser.class);
        List<Document> indexInfo = getIndexInfo(MethodMappedUser.class);
        Assert.assertEquals(indexInfo.size(), 3);
    }

    private void assertBackground(List<Document> indexInfo) {
        for (Document document : indexInfo) {
            if (!document.getString("name").equals("_id_")) {
                Assert.assertTrue(document.getBoolean("background"));
            }
        }
    }

    private void assertHashed(List<Document> indexInfo) {
        for (Document document : indexInfo) {
            if (!document.getString("name").equals("_id_")) {
                Assert.assertEquals(((Document) document.get("key")).get("hashedValue"), "hashed");
            }
        }
    }

    @Entity
    @Indexes(@Index(fields = {@Field("actor.actorObject.userId"), @Field(value = "actor.actorType", type = DESC)},
        options = @IndexOptions(disableValidation = true,
            partialFilter = "{ 'actor.actorObject.userId': { $exists: true }, 'actor.actorType': { $exists: true } }")))
    private static class FeedEvent {
        @Id
        private ObjectId id;
    }

    @Entity
    private static class InboxEvent {
        @Id
        private ObjectId id;
        private FeedEvent feedEvent;
    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(background = true),
        fields = @Field("name"))})
    private static class TestWithDeprecatedIndex {
        @Id
        private ObjectId id;
        private String name;

    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(), fields = {@Field(value = "hashedValue", type = IndexType.HASHED)})})
    private static class TestWithHashedIndex {
        @Id
        private ObjectId id;
        private String hashedValue;

    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(name = "collated",
        partialFilter = "{ name : { $exists : true } }",
        collation = @Collation(locale = "en_US", alternate = SHIFTED, backwards = true,
            caseFirst = CollationCaseFirst.UPPER, caseLevel = true, maxVariable = CollationMaxVariable.SPACE, normalization = true,
            numericOrdering = true, strength = CollationStrength.IDENTICAL),
        background = true),
        fields = {@Field(value = "name")})})
    private static class TestWithIndexOption {
        @Id
        private ObjectId id;
        private String name;

    }
}
