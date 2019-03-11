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

import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;
import org.bson.Document;
import org.junit.Test;
import dev.morphia.TestDatastore.FacebookUser;
import dev.morphia.query.Query;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapReduceOptionsTest extends TestBase {
    @Test
    @SuppressWarnings("deprecation")
    public void mapReduceCommand() {
        Query<FacebookUser> query = getDs().find(FacebookUser.class);
        MapReduceOptions<FacebookUser> options = new MapReduceOptions<FacebookUser>()
            .bypassDocumentValidation(true)
            .collation(Collation.builder().locale("en").build())
            .finalize("i'm a finalize function")
            .jsMode(true)
            .limit(42)
            .map("i'm a map function")
            .maxTimeMS(42000)
            .outputCollection("output collection")
            .outputDB("output db")
            .outputType(OutputType.INLINE)
            .query(query)
            .readPreference(ReadPreference.primaryPreferred())
            .reduce("i'm a reduce function")
            .scope(new Document("key", "value").append("key2", "value2"))
            .verbose(true);

        MapReduceCommand command = options.toCommand(getMorphia().getMapper());

        assertTrue(command.getBypassDocumentValidation());
        assertEquals(Collation.builder().locale("en").build(), command.getCollation());
        assertTrue(command.getJsMode());
        assertEquals(42, command.getLimit());
        assertEquals("i'm a map function", command.getMap());
        assertEquals(42000, command.getMaxTime(TimeUnit.MILLISECONDS));
        assertEquals("output collection", command.getOutputTarget());
        assertEquals("output db", command.getOutputDB());
        assertEquals(query.getQueryObject(), command.getQuery());
        assertEquals(query.getSortObject(), command.getSort());
        assertEquals(ReadPreference.primaryPreferred(), command.getReadPreference());
        assertEquals("i'm a map function", command.getMap());
        assertEquals("i'm a reduce function", command.getReduce());
        assertEquals("i'm a finalize function", command.getFinalize());
        assertEquals(new Document("key", "value").append("key2", "value2"), command.getScope());
        assertTrue(command.isVerbose());

    }
}
