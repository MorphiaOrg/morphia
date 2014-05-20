package org.mongodb.morphia.issue615;

import java.util.ArrayList;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FilterOperator;

import static org.junit.Assert.assertTrue;

/**
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

        boolean compatible = Mapper.isCompatibleForOperator(
                mf,
                mf.getType(),
                FilterOperator.EQUAL,
                new LocalUser());

        assertTrue("LocalUser should be compatible for field of type User", compatible);
    }
}
