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
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;

import java.util.List;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static org.junit.Assert.assertEquals;

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


}
