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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Martian;
import dev.morphia.test.models.User;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.AfterClass;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({ "unused", "MismatchedQueryAndUpdateOfCollection" })
public class AggregationTest extends TemplatedTestBase {

    public AggregationTest() {
        super(buildConfig(Martian.class, User.class)
                .applyIndexes(true)
                .codecProvider(new ZDTCodecProvider()));
    }

    @AfterClass
    public void testCoverage() {
        var type = getClass();
        var methods = stream(type.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("testExample"))
                .map(m -> {
                    String name = m.getName().substring(4);
                    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
                })
                .toList();

        if (methods.isEmpty()) {
            return;
        }
        String path = type.getPackageName();
        String simpleName = type.getSimpleName().substring(4);
        var operatorName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        var resourceFolder = new File("src/test/resources/%s/%s".formatted(path.replace('.', '/'), operatorName));

        if (!resourceFolder.exists()) {
            throw new IllegalStateException("%s does not exist inside %s".formatted(resourceFolder,
                    new File(".").getAbsolutePath()));
        }
        List<File> list = Arrays.stream(resourceFolder.list())
                .map(s -> new File(resourceFolder, s))
                .toList();

        List<String> examples = list.stream()
                .filter(d -> new File(d, "pipeline.json").exists())
                .map(File::getName)
                .toList();
        examples.forEach(example -> {
            assertTrue(methods.contains(example), "Missing test case for $%s: %s".formatted(operatorName, example));
        });
    }

    public void testPipeline(ServerVersion serverVersion,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        testPipeline(serverVersion, true, true, pipeline);
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
