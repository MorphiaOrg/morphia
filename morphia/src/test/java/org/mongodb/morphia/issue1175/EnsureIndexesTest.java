package org.mongodb.morphia.issue1175;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mongodb.morphia.testutil.IndexMatcher.doesNotHaveIndexNamed;
import static org.mongodb.morphia.testutil.IndexMatcher.hasIndexNamed;

import java.util.List;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import com.mongodb.DBObject;

public class EnsureIndexesTest extends TestBase {
    @Test
    public final void ensureIndexesFromSuperclasses() {
        // given
        getMorphia().map(Person.class, Employee.class, Company.class);

        // when
        getAds().ensureIndexes();

        // then
        List<DBObject> indexInfo = getDs().getCollection(Company.class).getIndexInfo();
        assertEquals(3, indexInfo.size());
        assertThat(indexInfo, hasIndexNamed("_id_"));
        assertThat(indexInfo, hasIndexNamed("employees.employeeId_1"));
        assertThat(indexInfo, hasIndexNamed("employees.fullName_1"));
    }

    @Test
    public final void ensureIndexesWithoutSubclasses() {
        // given
        getMorphia().map(Person.class, Employee.class, Contract.class);

        // when
        getAds().ensureIndexes();

        // then
        List<DBObject> indexInfo = getDs().getCollection(Contract.class).getIndexInfo();
        assertEquals(2, indexInfo.size());
        assertThat(indexInfo, hasIndexNamed("_id_"));
        assertThat(indexInfo, hasIndexNamed("person.fullName_1"));
        assertThat(indexInfo, doesNotHaveIndexNamed("person.employeeId_1"));
    }

    @Embedded
    public static class Person {
        @Indexed
        public String fullName;
    }

    @Embedded
    public static class Employee extends Person {
        @Indexed
        public Long employeeId;
        public String title;
    }

    @Entity
    public static class Contract {
        @Id
        public Long contractId;
        public Long companyId;
        @Embedded
        public Person person;
    }

    @Entity
    public static class Company {
        @Id
        public Long companyId;
        @Embedded
        public List<Employee> employees;
    }
}
