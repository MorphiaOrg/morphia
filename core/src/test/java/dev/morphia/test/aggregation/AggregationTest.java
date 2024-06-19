/*
 * Copyright (c) 2008 - 2013 MongoDB, Inc. <http://mongodb.com>
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

package dev.morphia.test.aggregation;

import java.util.List;
import java.util.function.Function;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Martian;
import dev.morphia.test.models.User;
import dev.morphia.test.util.Comparanator;

import org.bson.Document;

@SuppressWarnings({ "unused", "MismatchedQueryAndUpdateOfCollection" })
public class AggregationTest extends TemplatedTestBase {

    public AggregationTest() {
        super(buildConfig(Martian.class, User.class)
                .applyIndexes(true)
                .codecProvider(new ZDTCodecProvider()));
    }

    public void testPipeline(ServerVersion serverVersion,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        testPipeline(serverVersion, true, true, pipeline);
    }

    public void testPipeline(ServerVersion serverVersion,
            boolean removeIds,
            boolean orderMatters,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        checkMinServerVersion(serverVersion);
        checkMinDriverVersion(minDriver);
        var resourceName = discoverResourceName();
        loadData(resourceName, EXAMPLE_TEST_COLLECTION);
        loadIndex(resourceName, EXAMPLE_TEST_COLLECTION);

        List<Document> actual = runPipeline(resourceName, pipeline.apply(getDs().aggregate(EXAMPLE_TEST_COLLECTION)));

        if (!skipDataCheck) {
            List<Document> expected = loadExpected(resourceName);

            actual = removeIds ? removeIds(actual) : actual;
            expected = removeIds ? removeIds(expected) : expected;

            try {
                Comparanator.of(null, actual, expected, orderMatters).compare();
            } catch (AssertionError e) {
                throw new AssertionError("%s\n\n actual: %s".formatted(e.getMessage(), toString(actual, "\n\t")),
                        e);
            }
        }
    }

}
