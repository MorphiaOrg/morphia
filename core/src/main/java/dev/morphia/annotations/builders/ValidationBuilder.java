package dev.morphia.annotations.builders;

import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import dev.morphia.annotations.Validation;

/**
 * This is an internal class subject to change and removal.  Do not use.
 *
 * @morphia.internal
 */
public class ValidationBuilder extends AnnotationBuilder<Validation> implements Validation {
    /**
     * @param action Do not use.
     * @return Do not use.
     */
    public ValidationBuilder action(ValidationAction action) {
        put("action", action);
        return this;
    }

    @Override
    public Class<Validation> annotationType() {
        return Validation.class;
    }

    /**
     * @param level Do not use.
     * @return Do not use.
     */
    public ValidationBuilder level(ValidationLevel level) {
        put("level", level);
        return this;
    }

    @Override
    public String value() {
        return get("value");
    }

    @Override
    public ValidationLevel level() {
        return get("level");
    }

    @Override
    public ValidationAction action() {
        return get("action");
    }

    /**
     * @param value Do not use.
     * @return Do not use.
     */
    public ValidationBuilder value(String value) {
        put("value", value);
        return this;
    }
}
