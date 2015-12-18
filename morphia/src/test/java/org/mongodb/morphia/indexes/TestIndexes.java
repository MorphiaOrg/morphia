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
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import java.util.List;

public class TestIndexes extends TestBase {

    @Test
    public void testIndices() {

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

        datastore.ensureIndexes(TestWithIndexOption.class, true);
        Assert.assertEquals(2, indexOptionColl.getIndexInfo().size());
        assertBackground(indexOptionColl.getIndexInfo());

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
    @Indexes({@Index(options = @IndexOptions(), fields = {@Field(value = "name")})})
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
