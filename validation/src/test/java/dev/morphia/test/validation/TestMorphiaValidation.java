package dev.morphia.test.validation;

import java.util.List;

import dev.morphia.test.TestBase;
import dev.morphia.validation.ValidationExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMorphiaValidation extends TestBase {

    /**
     * Validation runs as a global listener configured in {@code MorphiaConfig.listeners}.
     */
    @Test
    public void testValidation() {
        withConfig(buildConfig(Data.class).listeners(List.of(new ValidationExtension())), () -> {
            final Data data = new Data();
            data.email = "not an email";

            try {
                getDs().save(data);
                Assertions.fail("Should have failed validation");
            } catch (ValidationException exception) {
                String message = exception.getMessage();
                assertTrue(message.contains("Data.email:must be a well-formed email address ('not an email')"), message);
            }

            data.email = "foo@bar.com";
            getDs().save(data);
        });
    }
}
