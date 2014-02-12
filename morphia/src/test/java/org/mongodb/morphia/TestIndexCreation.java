/*
 * Copyright (c) 2008 - 2014 MongoDB, Inc. <http://mongodb.com>
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

package org.mongodb.morphia;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.NotSaved;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestIndexCreation extends TestBase {
    @Entity
    @Indexes({
        @Index("test")
    })
    private static class HasIndex {
        @Id
        private ObjectId id;
        private String test;
    }

    @Entity
    private static class NoIndexes {
        @Id
        private ObjectId id;

        @NotSaved
        private HasIndex hasIndex;
    }

    @Test
    public void testNotSaved() {
        getMorphia().map(HasIndex.class, NoIndexes.class);
        Datastore ds = getDs();
        ds.ensureIndexes();
        ds.save(new HasIndex());
        ds.save(new NoIndexes());
        List<DBObject> indexes = getDb().getCollection("NoIndexes").getIndexInfo();
        assertEquals(1, indexes.size());
    }
}
