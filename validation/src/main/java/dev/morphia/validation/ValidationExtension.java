package dev.morphia.validation;

import java.util.Set;

import dev.morphia.Datastore;
import dev.morphia.PrePersistListener;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/**
 * Enables jakarta.validation support: entities are validated before they're saved. To turn it on, list it as a global
 * listener in the Morphia configuration:
 *
 * <pre>
 * morphia.listeners = dev.morphia.validation.ValidationExtension
 * </pre>
 *
 * or {@code config.listeners(List.of(new ValidationExtension()))} on a {@code MorphiaConfig}.
 *
 * @since 2.3
 */
public class ValidationExtension implements PrePersistListener<Object> {
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
    public void prePersist(Object ent, Datastore datastore) {
        final Set validate = validationFactory.getValidator().validate(ent);
        if (!validate.isEmpty()) {
            throw new VerboseJSR303ConstraintViolationException(validate);
        }
    }
}
