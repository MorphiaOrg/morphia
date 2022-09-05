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

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.AggregationImpl;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.bson.json.JsonWriterSettings.builder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
public class AggregationTest extends TestBase {

    protected final ObjectMapper mapper = new ObjectMapper();

    public void testPipeline(double serverVersion, String resourceName,
                             Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        testPipeline(serverVersion, resourceName, true, true, pipeline);
    }

    public void testPipeline(double serverVersion, String resourceName, boolean removeIds, boolean orderMatters,
                             Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        String collection = "aggtest";
        checkMinServerVersion(serverVersion);
        loadData(collection, resourceName);

        List<Document> documents = runPipeline(resourceName, pipeline.apply(getDs().aggregate(collection)));

        List<Document> actual = removeIds ? removeIds(documents) : documents;
        List<Document> expected = loadExpected(resourceName);

        if (orderMatters){
            assertEquals(actual, expected);
        } else {
            assertListEquals(actual, expected);
        }
    }

    protected void loadData(String collection, String resourceName) {
        insert(collection, loadJson(format("%s/%s/data.json", prefix(), resourceName)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected List<Document> runPipeline(String pipelineTemplate, Aggregation<Document> aggregation) {
        String pipelineName = format("%s/%s/pipeline.json", prefix(), pipelineTemplate);
        try {
            List<Document> pipeline = ((AggregationImpl) aggregation).pipeline();

            Iterator<Map<String, Object>> iterator = mapper.readValue(getClass().getResourceAsStream(pipelineName), List.class)
                                                           .iterator();
            for (Document stage : pipeline) {
                Object next = iterator.next();
                assertEquals(mapper.readValue(stage.toJson(), Map.class), next,
                    pipeline.stream()
                            .map(d -> d.toJson(builder()
                                                   .indent(true)
                                                   .build()))
                            .collect(Collectors.joining("\n", "[\n", "\n]")));
            }

            try (var cursor = aggregation.execute(Document.class)) {
                return cursor.toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    protected @NotNull List<Document> loadExpected(String resourceName) {
        return loadJson(format("%s/%s/expected.json", prefix(), resourceName));
    }

    @NotNull
    private List<Document> loadJson(String name) {
        List<Document> data = new ArrayList<>();
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            fail("missing data file: " + name);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (reader.ready()) {
                data.add(Document.parse(reader.readLine()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return data;
    }

    public final String prefix() {
        String root = getClass().getSimpleName().replace("Test", "");
        return toLowerCase(root.charAt(0)) + root.substring(1);
    }

    protected void cakeSales() {
        insert("cakeSales", parseDocs(
            "{ _id: 0, type: 'chocolate', orderDate: ISODate('2020-05-18T14:10:30Z'), state: 'CA', price: 13, quantity: 120 }",
            "{ _id: 1, type: 'chocolate', orderDate: ISODate('2021-03-20T11:30:05Z'), state: 'WA', price: 14, quantity: 140 }",
            "{ _id: 2, type: 'vanilla', orderDate: ISODate('2021-01-11T06:31:15Z'), state: 'CA', price: 12, quantity: 145 }",
            "{ _id: 3, type: 'vanilla', orderDate: ISODate('2020-02-08T13:13:23Z'), state: 'WA', price: 13, quantity: 104 }",
            "{ _id: 4, type: 'strawberry', orderDate: ISODate('2019-05-18T16:09:01Z'), state: 'CA', price: 41, quantity: 162 }",
            "{ _id: 5, type: 'strawberry', orderDate: ISODate('2019-01-08T06:12:03Z'), state: 'WA', price: 43, quantity: 134 }"));
    }

    @NotNull
    protected List<Document> parseDocs(String... strings) {
        return stream(strings)
                   .map(Document::parse)
                   .collect(toList());
    }

    protected void compare(int id, List<Document> expected, List<Document> actual) {
        assertEquals(find(id, actual), find(id, expected));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Document find(int id, List<Document> documents) {
        return documents.stream().filter(d -> d.getInteger("_id").equals(id)).findFirst().get();
    }

    private Document getDocument(Document document, String... path) {
        Document current = document;
        for (String step : path) {
            Object next = current.get(step);
            Assert.assertNotNull(next, format("Could not find %s in \n%s", step, current));
            current = (Document) next;
        }
        return current;
    }
}
