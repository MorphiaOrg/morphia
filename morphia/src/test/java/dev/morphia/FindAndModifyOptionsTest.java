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

package dev.morphia;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.DBCollectionFindAndModifyOptions;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FindAndModifyOptionsTest {
    @Test
    public void passThrough() {
        Collation collation = Collation.builder()
                                       .locale("en")
                                       .caseLevel(true)
                                       .build();
        DBCollectionFindAndModifyOptions options = new FindAndModifyOptions()
            .bypassDocumentValidation(true)
            .collation(collation).getOptions()
            .maxTime(15, TimeUnit.MINUTES)
            .projection(new BasicDBObject("field", "value"))
            .remove(true)
            .returnNew(true)
            .sort(new BasicDBObject("field", -1))
            .update(new BasicDBObject("$inc", "somefield"))
            .upsert(true)
            .writeConcern(WriteConcern.JOURNALED);

        assertTrue(options.getBypassDocumentValidation());
        assertEquals(collation, options.getCollation());
        assertEquals(15, options.getMaxTime(TimeUnit.MINUTES));
        assertEquals(new BasicDBObject("field", "value"), options.getProjection());
        assertTrue(options.isRemove());
        assertTrue(options.returnNew());
        assertEquals(new BasicDBObject("field", -1), options.getSort());
        assertEquals(new BasicDBObject("$inc", "somefield"), options.getUpdate());
        assertTrue(options.isUpsert());
        assertEquals(WriteConcern.JOURNALED, options.getWriteConcern());
    }
}
