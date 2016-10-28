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
import org.junit.Test;
import org.mongodb.morphia.entities.EmbeddedType;
import org.mongodb.morphia.entities.EntityWithListsAndArrays;

import java.util.TreeSet;

import static com.mongodb.BasicDBObject.parse;
import static java.util.Arrays.asList;

public class TestOnDiskFormat extends TestBase {
    @Test
    public void listsAndArrays() {
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

        String json = "{ \"_id\" : { \"$oid\" : \"581354ef5265cc2229ab72f2\" }, \"className\" : \"org.mongodb"
            + ".morphia.entities.EntityWithListsAndArrays\", \"arrayOfStrings\" : [\"first\", "
            + "\"2nd\", \"third\"], \"arrayOfInts\" : [1, 2, 3, 4, 5], \"listOfStrings\" "
            + ": [\"How\", \"does\", \"this\", \"look?\"], \"listEmbeddedType\" : [{ "
            + "\"number\" : { \"$numberLong\" : \"42\" }, \"text\" : \"Douglas Adams\" }, { \"number\" : { \"$numberLong\" : \"1\" }, "
            + "\"text\" : \"Love\" }], \"setOfIntegers\" : [1, 2, 3], \"notAnArrayOrList\" : \"So special\" }";
        Assert.assertEquals(parse(json), dbObject);
    }
}
