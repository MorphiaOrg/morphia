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

package dev.morphia.generics;

import dev.morphia.Datastore;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestJavaLists extends TestBase {
    @Test
    public void emptyModel() {
        getMapper().getOptions().setStoreEmpties(true);
        getMapper().getOptions().setStoreNulls(false);

        TestEmptyModel model = new TestEmptyModel();
        model.text = "text";
        model.wrapped = new TestEmptyModel.Wrapped();
        model.wrapped.text = "textWrapper";
        getDs().save(model);
        TestEmptyModel model2 = getDs().find(TestEmptyModel.class).filter("id", model.id)
                                       .execute(new FindOptions().limit(1))
                                       .next();
        assertNull(model.wrapped.others);
        assertNull(model2.wrapped.others);
    }

    @Test
    public void mapperTest() {
        Mapper.map(Employee.class);

        for (boolean nulls : new boolean[]{true, false}) {
            for (boolean empties : new boolean[]{true, false}) {
                getMapper().getOptions().setStoreNulls(nulls);
                getMapper().getOptions().setStoreEmpties(empties);
                empties();
            }
        }
    }

    private void empties() {
        Datastore ds = getDs();
        ds.delete(ds.find(Employee.class));
        Employee employee = new Employee();
        employee.byteList = asList((byte) 1, (byte) 2);
        ds.save(employee);

        Employee loaded = ds.find(Employee.class)
                            .execute(new FindOptions().limit(1))
                            .next();

        assertEquals(employee.byteList, loaded.byteList);
        assertNull(loaded.floatList);
    }

    @Entity
    static class TestEmptyModel {
        @Id
        private ObjectId id;
        private String text;
        private Wrapped wrapped;

        private static class Wrapped {
            private List<Wrapped> others;
            private String text;
        }
    }

    @Entity("employees")
    static class Employee {
        @Id
        private ObjectId id;

        private List<Float> floatList;
        private List<Byte> byteList;
    }
}

class JsonList {
    @Id
    private ObjectId id;
    private List<Object> jsonList;
    private List<Object> jsonObject;
}
