package dev.morphia.test.validation;

import dev.morphia.mapping.Mapper;
import dev.morphia.test.TestBase;
import dev.morphia.validation.ValidationExtension;

import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.validation.ValidationException;

import static org.testng.Assert.assertTrue;

@Test
public class TestMorphiaValidation extends TestBase {

    /**
     * Test method for {@link ValidationExtension#prePersist(Object, Document, Mapper)}.
     */
    public void testValidation() {
        final Data data = new Data();
        data.email = "not an email";

        getDs().getMapper().addInterceptor(new ValidationExtension());

        try {
            getDs().save(data);
            Assert.fail("Should have failed validation");
        } catch (ValidationException exception) {
            String message = exception.getMessage();
            assertTrue(message.contains("Data.email:must be a well-formed email address ('not an email')"), message);
        }

        data.email = "foo@bar.com";
        getDs().save(data);
    }
}
