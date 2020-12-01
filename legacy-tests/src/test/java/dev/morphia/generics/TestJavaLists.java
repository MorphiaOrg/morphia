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
import dev.morphia.DeleteOptions;
import dev.morphia.Morphia;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.FindOptions;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestJavaLists extends TestBase {
    @Test
    public void emptyModel() {
        MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                             .storeEmpties(true)
                                             .storeNulls(false)
                                             .build();
        final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);

        TestEmptyModel model = new TestEmptyModel();
        model.text = "text";
        model.wrapped = new TestEmptyModel.Wrapped();
        model.wrapped.text = "textWrapper";
        datastore.save(model);
        TestEmptyModel model2 = getDs().find(TestEmptyModel.class)
                                       .filter(eq("id", model.id)).iterator(new FindOptions().limit(1))
                                       .next();
        assertNull(model.wrapped.others);
        assertNull(model2.wrapped.others);
    }

    @Test
    public void mapperTest() {
        getMapper().map(Employee.class);

        for (boolean nulls : new boolean[]{true, false}) {
            for (boolean empties : new boolean[]{true, false}) {
                MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                                     .storeNulls(nulls)
                                                     .storeEmpties(empties)
                                                     .build();
                final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);
                empties(datastore);
            }
        }
    }

    private void empties(Datastore ds) {
        ds.find(Employee.class).delete(new DeleteOptions().multi(true));
        Employee employee = new Employee();
        employee.byteList = asList((byte) 1, (byte) 2);
        ds.save(employee);

        Employee loaded = ds.find(Employee.class).iterator(new FindOptions().limit(1))
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

        @Entity
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

@Entity
class JsonList {
    @Id
    private ObjectId id;
    private List<Object> jsonList;
    private List<Object> jsonObject;
}
