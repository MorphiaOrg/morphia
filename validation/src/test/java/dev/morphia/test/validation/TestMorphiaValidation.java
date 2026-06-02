package dev.morphia.test.validation;

import dev.morphia.mapping.Mapper;
import dev.morphia.test.TestBase;
import dev.morphia.validation.ValidationExtension;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMorphiaValidation extends TestBase {

    /**
     * Test method for {@link ValidationExtension#prePersist(Object, Document, Mapper)}.
     */
    @Test
    public void testValidation() {
        final Data data = new Data();
        data.email = "not an email";

        getDs().getMapper().addInterceptor(new ValidationExtension());

        try {
            getDs().save(data);
            Assertions.fail("Should have failed validation");
        } catch (ValidationException exception) {
            String message = exception.getMessage();
            assertTrue(message.contains("Data.email:must be a well-formed email address ('not an email')"), message);
        }

        data.email = "foo@bar.com";
        getDs().save(data);
    }
}
