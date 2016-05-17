/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

package org.mongodb.morphia.converters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.testutil.TestEntity;

public class TestStoringMapKeyOrdering extends TestBase {

    @Test
    public void testKeyOrdering() {
        getMorphia().map(LinkedHashMapTestEntity.class);
        final LinkedHashMapTestEntity expectedEntity = new LinkedHashMapTestEntity();
        for (int i = 100; i >= 0; i--) {
            expectedEntity.getLinkedHashMap().put(i, "a" + i);
        }
        getDs().save(expectedEntity);
        LinkedHashMapTestEntity storedEntity = getDs().find(LinkedHashMapTestEntity.class).get();
        Assert.assertNotNull(storedEntity);
        Assert.assertEquals(
                new ArrayList<Integer>(expectedEntity.getLinkedHashMap().keySet()),
                new ArrayList<Integer>(storedEntity.getLinkedHashMap().keySet()));
    }
}

@Entity
class LinkedHashMapTestEntity extends TestEntity {
    @Embedded(concreteClass = java.util.LinkedHashMap.class)
    private final Map<Integer, String> linkedHashMap = new LinkedHashMap<Integer, String>();

    public Map<Integer, String> getLinkedHashMap() {
        return linkedHashMap;
    }
}
