package dev.morphia;

import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.Email;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.annotations.Id;

import javax.validation.ValidationException;


/**
 * @author doc
 */
public class TestMorphiaValidation extends TestBase {

    @Entity
    public static class E {
        @Id
        private ObjectId id;
        @Email
        private String email;
    }

    /**
     * Test method for {@link ValidationExtension#prePersist(Object, Document, Mapper)}.
     */
    @Test
    public final void testPrePersist() {
        final E e = new E();
        e.email = "not an email";

        new ValidationExtension(getDs().getMapper());

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
