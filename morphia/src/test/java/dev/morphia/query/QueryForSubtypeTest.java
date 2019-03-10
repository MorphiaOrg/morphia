package dev.morphia.query;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.validation.ValidationFailure;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.SIZE;
import static dev.morphia.query.QueryValidator.isCompatibleForOperator;

/**
 * For issue #615.
 *
 * @author jbyler
 */
public class QueryForSubtypeTest extends TestBase {

    private MappedClass jobMappedClass;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        jobMappedClass = new Mapper().getMappedClass(Job.class);
    }

    @Test
    public void testImplementingClassIsCompatibleWithInterface() {
        MappedField user = jobMappedClass.getMappedField("owner");

        boolean compatible = isCompatibleForOperator(jobMappedClass,
                                                     user,
                                                     User.class,
                                                     EQUAL,
                                                     new UserImpl(),
                                                     new ArrayList<ValidationFailure>());

        assertThat(compatible, is(true));
    }

    @Test
    public void testIntSizeShouldBeCompatibleWithArrayList() {
        MappedField attributes = jobMappedClass.getMappedField("attributes");

        boolean compatible = isCompatibleForOperator(jobMappedClass,
                                                     attributes,
                                                     ArrayList.class,
                                                     SIZE,
                                                     2,
                                                     new ArrayList<ValidationFailure>());

        assertThat(compatible, is(true));
    }

    @Test
    public void testSubclassOfKeyShouldBeCompatibleWithFieldUser() {
        MappedField user = jobMappedClass.getMappedField("owner");
        Key<User> anonymousKeySubclass = new Key<User>(User.class, "User", 212L) {
        };

        boolean compatible = isCompatibleForOperator(jobMappedClass,
                                                     user,
                                                     User.class,
                                                     EQUAL,
                                                     anonymousKeySubclass,
                                                     new ArrayList<ValidationFailure>());

        assertThat(compatible, is(true));
    }

    interface User {
    }

    @Entity
    static class UserImpl implements User {
        @Id
        @SuppressWarnings("unused")
        private ObjectId id;
    }

    @Entity
    static class Job {
        @Id
        @SuppressWarnings("unused")
        private ObjectId id;

        @Reference
        @SuppressWarnings("unused")
        private User owner;

        @SuppressWarnings("unused")
        private ArrayList<String> attributes;
    }
}
