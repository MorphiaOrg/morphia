package org.mongodb.morphia.query;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.validation.ValidationFailure;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

/**
 * For issue #615.
 * 
 * @author jbyler
 */
public class QueryForSubtypeTest extends TestBase {

    private MappedClass jobMappedClass;

    interface User { }

    @Entity
    static class LocalUser implements User {
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

    @Before
    public void commonSetup() throws Exception {
        Mapper mapper = new Mapper();
        jobMappedClass = mapper.getMappedClass(Job.class);
    }

    @Test
    public void testClassIsCompatibleWithInterface() {
        MappedField mf = jobMappedClass.getMappedField("owner");

        boolean compatible = QueryValidator.isCompatibleForOperator(
                mf,
                mf.getType(),
                FilterOperator.EQUAL,
                new LocalUser(), 
                new ArrayList<ValidationFailure>());

        assertTrue("LocalUser should be compatible for field of type User", compatible);
    }

    @Test
    public void testSizeOfArrayList() {
        MappedField mf = jobMappedClass.getMappedField("attributes");

        boolean compatible = QueryValidator.isCompatibleForOperator(
                mf,
                mf.getType(),
                FilterOperator.SIZE,
                2,
                new ArrayList<ValidationFailure>());

        assertTrue("$size 2 should be compatible for field of type ArrayList", compatible);
    }

    @Test
    public void testSubclassOfKey() {
        MappedField mf = jobMappedClass.getMappedField("owner");

        boolean compatible = QueryValidator.isCompatibleForOperator(
                mf,
                mf.getType(),
                FilterOperator.EQUAL,
                new Key<User>(User.class, 212L) {},
                new ArrayList<ValidationFailure>()); // anonymous subclass of Key

        assertTrue("Subclass of Key<User> should be compatible for field of type User", compatible);
    }
}
