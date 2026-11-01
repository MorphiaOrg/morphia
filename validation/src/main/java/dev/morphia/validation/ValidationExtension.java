package dev.morphia.validation;

import java.lang.annotation.Annotation;
import java.util.Set;

import com.mongodb.lang.NonNull;

import dev.morphia.EntityListener;
import dev.morphia.annotations.PrePersist;

import org.bson.Document;

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
 * or {@code MorphiaConfig.listeners(List.of(ValidationExtension.class))}.
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
    public boolean hasAnnotation(@NonNull Class<? extends Annotation> type) {
        return PrePersist.class.equals(type);
    }

    /**
     * Validates an entity before it's saved. It takes the entity alone, so entities can be encoded without building a
     * {@code Document}.
     *
     * @param entity the entity
     * @since 3.0
     */
    @PrePersist
    @SuppressWarnings("unchecked")
    public void validate(Object entity) {
        final Set validate = validationFactory.getValidator().validate(entity);
        if (!validate.isEmpty()) {
            throw new VerboseJSR303ConstraintViolationException(validate);
        }
    }

    /**
     * Validates an entity when this extension is added with the deprecated {@code Mapper.addInterceptor}.
     */
    @Override
    public void prePersist(Object ent, Document document, Object datastore) {
        validate(ent);
    }
}
