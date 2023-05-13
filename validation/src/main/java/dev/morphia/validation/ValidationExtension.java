package dev.morphia.validation;

import java.lang.annotation.Annotation;
import java.util.Set;

import dev.morphia.EntityInterceptor;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.Mapper;

import org.bson.Document;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

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
    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return PrePersist.class.equals(type);
    }

    @Override
    @PrePersist
    @SuppressWarnings("unchecked")
    public void prePersist(Object ent, Document document, Mapper mapper) {
        final Set validate = validationFactory.getValidator().validate(ent);
        if (!validate.isEmpty()) {
            throw new VerboseJSR303ConstraintViolationException(validate);
        }
    }
}
