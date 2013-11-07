package org.mongodb.morphia;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.Email;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Id;

import javax.validation.ValidationException;


/**
 * @author doc
 */
public class TestMorphiaValidation extends TestBase {

    public static class E {
        @Id
        private ObjectId id;
        @Email
        private String email;
    }

    /**
     * Test method for {@link ValidationExtension#prePersist(Object, com.mongodb.DBObject, org.mongodb.morphia.mapping.Mapper)}.
     */
    @Test
    public final void testPrePersist() {
        final E e = new E();
        e.email = "not an email";

        new ValidationExtension(getMorphia());

        try {
            getDs().save(e);
            Assert.fail("Should have failed validation");
        } catch (ValidationException exception) {
            // this is fine
        }

        e.email = "foo@bar.com";
        getDs().save(e);
    }
}
