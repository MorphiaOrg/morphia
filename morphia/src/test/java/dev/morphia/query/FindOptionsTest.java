/*
 * Copyright 2016 MongoDB, Inc.
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

package dev.morphia.query;

import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.DBCollectionFindOptions;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FindOptionsTest {
    @Test
    public void passThrough() {
        Collation collation = Collation.builder()
                                       .locale("en")
                                       .caseLevel(true)
                                       .build();
        DBCollectionFindOptions options = new FindOptions()
            .batchSize(42)
            .limit(18)
            .projection(new BasicDBObject("field", "value"))
            .maxTime(15, TimeUnit.MINUTES)
            .maxAwaitTime(45, TimeUnit.SECONDS)
            .skip(12)
            .sort(new BasicDBObject("field", -1))
            .cursorType(CursorType.TailableAwait)
            .noCursorTimeout(true)
            .oplogReplay(true)
            .partial(true)
            .readPreference(ReadPreference.secondaryPreferred(2, TimeUnit.MINUTES))
            .readConcern(ReadConcern.LOCAL)
            .collation(collation).getOptions();

        assertEquals(42, options.getBatchSize());
        assertEquals(18, options.getLimit());
        assertEquals(new BasicDBObject("field", "value"), options.getProjection());
        assertEquals(15, options.getMaxTime(TimeUnit.MINUTES));
        assertEquals(45, options.getMaxAwaitTime(TimeUnit.SECONDS));
        assertEquals(12, options.getSkip());
        assertEquals(new BasicDBObject("field", -1), options.getSort());
        assertEquals(CursorType.TailableAwait, options.getCursorType());
        assertTrue(options.isNoCursorTimeout());
        assertTrue(options.isOplogReplay());
        assertTrue(options.isPartial());
        assertEquals(ReadPreference.secondaryPreferred(2, TimeUnit.MINUTES), options.getReadPreference());
        assertEquals(ReadConcern.LOCAL, options.getReadConcern());
        assertEquals(collation, options.getCollation());
    }
}
