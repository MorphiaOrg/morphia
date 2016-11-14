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

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.mongodb.morphia.entities.EmbeddedType;
import org.mongodb.morphia.entities.EntityWithListsAndArrays;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.TreeSet;

import static com.mongodb.BasicDBObject.parse;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.mongodb.morphia.converters.DefaultConverters.JAVA_8;

@SuppressWarnings("Since15")
public class TestOnDiskFormat extends TestBase {
    @Test
    public void listsAndArrays() {
        Assume.assumeTrue("This test requires Java 8", JAVA_8);
        EntityWithListsAndArrays entity = new EntityWithListsAndArrays();
        entity.setId(new ObjectId("581354ef5265cc2229ab72f2"));
        entity.setArrayOfStrings(new String[]{"first", "2nd", "third"});
        entity.setArrayOfInts(new int[]{1, 2, 3, 4, 5});
        entity.setListOfStrings(asList("How", "does", "this", "look?"));
        entity.setListEmbeddedType(asList(new EmbeddedType(42L, "Douglas Adams"), new EmbeddedType(1L, "Love")));
        entity.setSetOfIntegers(new TreeSet<Integer>(asList(1, 2, 3)));
        entity.setNotAnArrayOrList("So special");

        getDs().save(entity);

        DBCollection collection = getDs().getCollection(EntityWithListsAndArrays.class);
        DBObject dbObject = collection.findOne();

        Assert.assertEquals(parse(readFully("/EntityWithListsAndArrays.json")), dbObject);
    }

    private String readFully(final String name) {
        return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(name)))
            .lines()
            .collect(joining("\n"));
    }
}
