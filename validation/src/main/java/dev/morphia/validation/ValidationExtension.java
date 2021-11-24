package dev.morphia.validation;


import dev.morphia.EntityInterceptor;
import dev.morphia.mapping.Mapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.bson.Document;

import java.util.Set;


/**
 * Enables jakarta.validation support.
 *
 * @since 2.3
 */
public class ValidationExtension implements EntityInterceptor {
    private final ValidatorFactory validationFactory;

    /**
     * Creates a ValidationExtension
     */
    public ValidationExtension() {
        validationFactory = Validation.byDefaultProvider()
                                      .configure()
                                      .buildValidatorFactory();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void prePersist(Object ent, Document document, Mapper mapper) {
        final Set validate = validationFactory.getValidator().validate(ent);
        if (!validate.isEmpty()) {
            throw new VerboseJSR303ConstraintViolationException(validate);
        }
    }
}
