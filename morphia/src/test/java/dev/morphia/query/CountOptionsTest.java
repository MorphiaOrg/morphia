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

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.DBCollectionCountOptions;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class CountOptionsTest {
    @Test
    public void passThrough() {
        Collation collation = Collation.builder()
                                       .locale("en")
                                       .caseLevel(true)
                                       .build();
        DBCollectionCountOptions options = new CountOptions()
            .collation(collation)
            .hint("i'm a hint")
            .limit(18)
            .maxTime(15, TimeUnit.MINUTES)
            .readPreference(ReadPreference.secondaryPreferred())
            .readConcern(ReadConcern.LOCAL)
            .skip(12)
            .getOptions();

        assertEquals(collation, options.getCollation());
        assertEquals("i'm a hint", options.getHintString());
        assertEquals(18, options.getLimit());
        assertEquals(15, options.getMaxTime(TimeUnit.MINUTES));
        assertEquals(ReadPreference.secondaryPreferred(), options.getReadPreference());
        assertEquals(ReadConcern.LOCAL, options.getReadConcern());
        assertEquals(12, options.getSkip());
    }
}
