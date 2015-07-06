/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */


package org.mongodb.morphia;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.query.UpdateResults;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author Scott Hernandez
 */
public class TestFrontPageExample extends TestBase {

    @Test
    public void testIt() throws Exception {
        getMorphia().map(Employee.class);

        getDs().save(new Employee("Mister", "GOD", null, 0));

        final Employee boss = getDs().find(Employee.class).field("manager").equal(null).get(); // get an employee without a manager
        Assert.assertNotNull(boss);
        final Key<Employee> key = getDs().save(new Employee("Scott", "Hernandez", getDs().getKey(boss), 150 * 1000));
        Assert.assertNotNull(key);

        final UpdateResults res = getDs().update(boss, getDs().createUpdateOperations(Employee.class)
                                                              .add("underlings", key)); //add Scott as an employee of his manager
        Assert.assertNotNull(res);
        Assert.assertTrue("Should update existing document", res.getUpdatedExisting());
        Assert.assertEquals("Should update one document", 1, res.getUpdatedCount());

        final Employee scottsBoss = getDs().find(Employee.class).filter("underlings", key).get(); // get Scott's boss
        Assert.assertNotNull(scottsBoss);
        Assert.assertEquals(boss.id, scottsBoss.id);

        for (final Employee e : getDs().find(Employee.class, "manager", boss)) {
            print();
        }
    }

    private void print() {
    }

    @Entity("employees")
    private static class Employee {

        @Reference
        private final List<Employee> underlings = new ArrayList<Employee>(); // refs are stored*, and loaded automatically
        private final transient boolean stored = true; // not @Transient, will be ignored by Serialization/GWT for example.
        @Id
        private ObjectId id; // auto-generated, if not set (see ObjectId)
        private String firstName;
        // Address address; // by default fields are @Embedded
        private String lastName; // value types are automatically persisted
        private Long salary; // only non-null values are stored
        private Key<Employee> manager; // references can be saved without automatic
        @Property("started")
        private Date startDate; // fields can be renamed
        @Property("left")
        private Date endDate;
        @Indexed
        private boolean active; // fields can be indexed for better performance
        @NotSaved
        private String readButNotStored; // fields can loaded, but not saved
        @Transient
        private int notStored; // fields can be ignored (no load/save)

        public Employee() {
        }

        public Employee(final String f, final String l, final Key<Employee> boss, final long sal) {
            firstName = f;
            lastName = l;
            manager = boss;
            salary = sal;
        }
    }
}
