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

package org.mongodb.morphia;

import com.mongodb.WriteConcern;
import org.junit.Test;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.ArrayList;

import static java.util.Arrays.asList;

public class TestAdvancedDatastore extends TestBase {
    @Test
    public void testBulkInsertEmptyIterable() {
        this.getAds().insert("class_1_collection", new ArrayList<TestEntity>());
    }

    @Test
    public void testBulkInsertWithoutCollection() {
        this.getAds().insert(asList(new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()),
                             WriteConcern.NORMAL);
        this.getAds().insert(new ArrayList<TestEntity>(), WriteConcern.NORMAL);
    }

    @Test
    public void testBulkInsertWithNullWC() {
        this.getAds().insert(new ArrayList<TestEntity>(), null);
    }

    @Test
    public void testBulkInsertEmptyVararg() {
        this.getAds().insert();
    }
}
