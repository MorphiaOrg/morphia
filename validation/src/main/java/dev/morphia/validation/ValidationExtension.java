package dev.morphia.validation;

import java.lang.annotation.Annotation;
import java.util.Set;

import dev.morphia.Datastore;
import dev.morphia.EntityListener;
import dev.morphia.annotations.PrePersist;

import org.bson.Document;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/**
 * Enables jakarta.validation support.
 *
 * @since 2.3
 */
public class ValidationExtension implements EntityListener<Object> {
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
    public void prePersist(Object ent, Document document, Datastore datastore) {
        final Set validate = validationFactory.getValidator().validate(ent);
        if (!validate.isEmpty()) {
            throw new VerboseJSR303ConstraintViolationException(validate);
        }
    }
}
