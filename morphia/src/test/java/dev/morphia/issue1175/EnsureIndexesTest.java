package dev.morphia.issue1175;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static dev.morphia.testutil.IndexMatcher.doesNotHaveIndexNamed;
import static dev.morphia.testutil.IndexMatcher.hasIndexNamed;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;

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
        assertEquals(4, indexInfo.size());
        assertThat(indexInfo, hasIndexNamed("_id_"));
        assertThat(indexInfo, hasIndexNamed("employees.birthday_-1"));
        assertThat(indexInfo, hasIndexNamed("employees.fullName_1"));
        assertThat(indexInfo, hasIndexNamed("employees.employeeId_1"));
    }

    @Test
    public final void ensureIndexesWithoutSubclasses() {
        // given
        getMorphia().map(Person.class, Employee.class, Contract.class);

        // when
        getAds().ensureIndexes();

        // then
        List<DBObject> indexInfo = getDs().getCollection(Contract.class).getIndexInfo();
        assertEquals(3, indexInfo.size());
        assertThat(indexInfo, hasIndexNamed("_id_"));
        assertThat(indexInfo, hasIndexNamed("person.birthday_-1"));
        assertThat(indexInfo, hasIndexNamed("person.fullName_1"));
        assertThat(indexInfo, doesNotHaveIndexNamed("person.employeeId_1"));
    }

    @Embedded
    public static class LivingBeing {
        @Indexed(IndexDirection.DESC)
        private Date birthday;
    }

    @Embedded
    public static class Person extends LivingBeing {
        @Indexed
        private String fullName;
    }

    @Embedded
    public static class Employee extends Person {
        @Indexed
        private Long employeeId;
        private String title;
    }

    @Entity
    public static class Contract {
        @Id
        private Long contractId;
        private Long companyId;
        @Embedded
        private Person person;
    }

    @Entity
    public static class Company {
        @Id
        private Long companyId;
        @Embedded
        private List<Employee> employees;
    }
}
