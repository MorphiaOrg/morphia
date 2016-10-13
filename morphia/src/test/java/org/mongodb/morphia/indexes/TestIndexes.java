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

package org.mongodb.morphia.indexes;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Collation;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import java.util.List;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;

public class TestIndexes extends TestBase {

    @Test
    public void testIndexes() {

        final Datastore datastore = getDs();
        datastore.delete(datastore.createQuery(TestWithIndexOption.class));

        final DBCollection indexOptionColl = getDb().getCollection(TestWithIndexOption.class.getSimpleName());
        indexOptionColl.drop();
        Assert.assertEquals(0, indexOptionColl.getIndexInfo().size());

        final DBCollection depIndexColl = getDb().getCollection(TestWithDeprecatedIndex.class.getSimpleName());
        depIndexColl.drop();
        Assert.assertEquals(0, depIndexColl.getIndexInfo().size());

        final DBCollection hashIndexColl = getDb().getCollection(TestWithHashedIndex.class.getSimpleName());
        hashIndexColl.drop();
        Assert.assertEquals(0, hashIndexColl.getIndexInfo().size());

        if (serverIsAtLeastVersion(3.4)) {
            datastore.ensureIndexes(TestWithIndexOption.class, true);
            Assert.assertEquals(2, indexOptionColl.getIndexInfo().size());
            List<DBObject> indexInfo = indexOptionColl.getIndexInfo();
            assertBackground(indexInfo);
            for (DBObject dbObject : indexInfo) {
                if (dbObject.get("name").equals("collated")) {
                    BasicDBObject collation = (BasicDBObject) dbObject.get("collation");
                    Assert.assertEquals("en_US", collation.get("locale"));
                    Assert.assertEquals("upper", collation.get("caseFirst"));
                    Assert.assertEquals("shifted", collation.get("alternate"));
                    Assert.assertTrue(collation.getBoolean("backwards"));
                    Assert.assertEquals("upper", collation.get("caseFirst"));
                    Assert.assertTrue(collation.getBoolean("caseLevel"));
                    Assert.assertEquals("space", collation.get("maxVariable"));
                    Assert.assertTrue(collation.getBoolean("normalization"));
                    Assert.assertTrue(collation.getBoolean("numericOrdering"));
                    Assert.assertEquals(5, collation.get("strength"));
                }
            }
        }

        datastore.ensureIndexes(TestWithDeprecatedIndex.class, true);
        Assert.assertEquals(2, depIndexColl.getIndexInfo().size());
        assertBackground(depIndexColl.getIndexInfo());

        datastore.ensureIndexes(TestWithHashedIndex.class);
        Assert.assertEquals(2, hashIndexColl.getIndexInfo().size());
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
                Assert.assertEquals(((DBObject) index.get("key")).get("hashedValue"), "hashed");
            }
        }
    }

    @Entity(noClassnameStored = true)
    @Indexes({@Index(options = @IndexOptions(name = "collated",
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
