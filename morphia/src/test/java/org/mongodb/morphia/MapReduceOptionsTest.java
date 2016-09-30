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

package org.mongodb.morphia;

import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;
import org.bson.Document;
import org.junit.Test;
import org.mongodb.morphia.TestDatastore.FacebookUser;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class MapReduceOptionsTest extends TestBase {
    @Test
    public void mapReduceCommand() {
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
            .query(getDs().createQuery(FacebookUser.class))
            .readPreference(ReadPreference.primaryPreferred())
            .reduce("i'm a reduce function")
            .resultType(FacebookUser.class)
            .scope(new Document("key", "value").append("key2", "value2"))
            .verbose(true);

        MapReduceCommand command = options.toCommand();

        assertEquals(options.getBypassDocumentValidation(), command.getBypassDocumentValidation());
        assertEquals(options.getCollation(), command.getCollation());
        assertEquals(options.getFinalize(), command.getFinalize());
        assertEquals(options.getJsMode(), command.getJsMode());
        assertEquals(options.getLimit(), command.getLimit());
        assertEquals(options.getMap(), command.getMap());
        assertEquals(options.getMaxTimeMS(), command.getMaxTime(TimeUnit.MILLISECONDS));
        assertEquals(options.getOutputCollection(), command.getOutputTarget());
        assertEquals(options.getOutputDB(), command.getOutputDB());
        assertEquals(options.getQuery().getQueryObject(), command.getQuery());
        assertEquals(options.getQuery().getSortObject(), command.getSort());
        assertEquals(options.getReadPreference(), command.getReadPreference());
        assertEquals(options.getReduce(), command.getReduce());
        assertEquals(options.getScope(), command.getScope());
        assertEquals(options.getVerbose(), command.isVerbose());

    }
}