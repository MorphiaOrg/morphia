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
import java.util.stream.Collectors;

import dev.morphia.aggregation.Aggregation;
import dev.morphia.test.ServerVersion;
import dev.morphia.test.TemplatedTestBase;
import dev.morphia.test.aggregation.model.Martian;
import dev.morphia.test.models.User;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterClass;

import static java.util.Arrays.stream;
import static org.testng.Assert.fail;

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
        String path = type.getPackageName();
        String simpleName = type.getSimpleName().substring(4);
        var operatorName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        var resourceFolder = rootToCore("src/test/resources/%s/%s".formatted(path.replace('.', '/'), operatorName));

        if (!resourceFolder.exists()) {
            throw new IllegalStateException("%s does not exist inside %s".formatted(resourceFolder,
                    new File(".").getAbsolutePath()));
        }
        List<File> list = Arrays.stream(resourceFolder.list())
                .map(s -> new File(resourceFolder, s))
                .toList();

        List<String> examples = list.stream()
                .filter(d -> new File(d, "expected.json").exists())
                .map(File::getName)
                .toList();
        var missing = examples.stream()
                .filter(example -> !methods.contains(example))
                .collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            fail("Missing test cases for $%s: %s".formatted(operatorName, missing));
        }
    }

    @NotNull
    public static File rootToCore(String path) {
        return new File(CORE_ROOT, path);
    }

    public void testPipeline(ServerVersion serverVersion,
            Function<Aggregation<Document>, Aggregation<Document>> pipeline) {
        testPipeline(serverVersion, true, true, pipeline);
    }

}
