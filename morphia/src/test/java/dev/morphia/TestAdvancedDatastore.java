/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

package dev.morphia;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import dev.morphia.testutil.TestEntity;
import org.junit.Assert;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class TestAdvancedDatastore extends TestBase {
    @Test
    public void testInsert() {
        MongoCollection collection = getDs().getCollection(TestEntity.class);
        this.getAds().insert(new TestEntity());
        Assert.assertEquals(1, collection.countDocuments());
        this.getAds().insert(new TestEntity(), new InsertOptions()
            .writeConcern(WriteConcern.ACKNOWLEDGED));
        Assert.assertEquals(2, collection.countDocuments());
    }

    @Test
    public void testBulkInsert() {

        MongoCollection collection = getDs().getCollection(TestEntity.class);
        this.getAds().insert(asList(new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()),
                             new InsertOptions().writeConcern(WriteConcern.ACKNOWLEDGED));
        Assert.assertEquals(5, collection.countDocuments());

        collection.drop();
        this.getAds().insert(asList(new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()),
                             new InsertOptions()
                                 .writeConcern(WriteConcern.ACKNOWLEDGED));
        Assert.assertEquals(5, collection.countDocuments());
    }

    @Test
    public void testInsertEmpty() {
        this.getAds().insert(emptyList());
        this.getAds().insert(emptyList(), new InsertOptions()
            .writeConcern(WriteConcern.ACKNOWLEDGED));
    }
}
