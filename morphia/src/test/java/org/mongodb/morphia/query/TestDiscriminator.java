/*
 * Copyright (c) 2008 - 2014 MongoDB, Inc. <http://mongodb.com>
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

package org.mongodb.morphia.query;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Discriminator;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class TestDiscriminator extends TestBase {

    @Entity(value = "people", noClassnameStored = true)
    @Discriminator(column = "type", value = "Person")
    public static class Person {
        @Id
        private ObjectId id;

        private String name;

        private boolean gender;

        private String type;

        protected Person(){

        }

        public Person(final String name, final boolean gender) {
            this.name = name;
            this.gender = gender;
        }

        public ObjectId getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isGender() {
            return gender;
        }

        public String getType() {
            return type;
        }
    }

    @Entity(value = "people", noClassnameStored = true)
    @Discriminator(column = "type", value = "Employee")
    public static class Employee extends Person {

        private String department;
        private String title;

        protected Employee() {
        }

        public Employee(final String name, final boolean gender, final String department, final String title) {
            super(name, gender);
            this.department = department;
            this.title = title;
        }

        public String getDepartment() {
            return department;
        }

        public String getTitle() {
            return title;
        }
    }

    @Test
    public void testSave() throws Exception {

        Employee employee = new Employee("Mike", true, "IT", "Dev");

        getDs().ensureIndexes(Employee.class);
        getDs().save(employee);

        Employee persisted = getDs().createQuery(Employee.class).field("department").equal("IT").get();

        assertNotNull(persisted);
        assertThat(persisted.getTitle(), is("Dev"));
        assertThat(persisted.getType(), is("Employee"));
    }

    @Test
    public void testQuery() throws Exception {
        Person person = new Person("Rich", true);
        getDs().save(person);

        Employee employee = new Employee("Mike", true, "IT", "Dev");
        getDs().save(employee);

        List<Employee> employees = getDs().createQuery(Employee.class).field("department").equal("IT").asList();
        assertThat(employees.size(), is(1));

        Employee persisted = getDs().createQuery(Employee.class).field("department").equal("IT").get();

        assertNotNull(persisted);
        assertThat(persisted.getTitle(), is("Dev"));
        assertThat(persisted.getType(), is("Employee"));
    }

    @Test
    public void testRemove() throws Exception {
        Person person = new Person("Rich", true);
        getDs().save(person);

        Employee employee = new Employee("Mike", true, "IT", "Dev");
        getDs().save(employee);

        Query<Employee> employeeQuery = getDs().createQuery(Employee.class).field("department").equal("IT");
        getDs().delete(employeeQuery);

        Employee employeePersisted = employeeQuery.get();
        assertNull(employeePersisted);

        Query<Person> personQuery = getDs().createQuery(Person.class).field("name").equal("Rich");
        Person personPersisted = personQuery.get();

        assertNotNull(personPersisted);
        assertThat(personPersisted.getName(), is("Rich"));
        assertThat(personPersisted.getType(), is("Person"));
        assertThat(personPersisted.isGender(), is(true));
    }
}
