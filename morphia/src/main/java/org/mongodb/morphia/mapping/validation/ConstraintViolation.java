package org.mongodb.morphia.mapping.validation;


import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ConstraintViolation {
    /**
     * Levels of constraint violations
     */
    public enum Level {
        MINOR,
        INFO,
        WARNING,
        SEVERE,
        FATAL
    }

    private final MappedClass clazz;
    private MappedField field;
    private final Class<? extends ClassConstraint> validator;
    private final String message;
    private final Level level;

    public ConstraintViolation(final Level level, final MappedClass clazz, final MappedField field,
                               final Class<? extends ClassConstraint> validator, final String message) {
        this(level, clazz, validator, message);
        this.field = field;
    }

    public ConstraintViolation(final Level level, final MappedClass clazz, final Class<? extends ClassConstraint> validator,
                               final String message) {
        this.level = level;
        this.clazz = clazz;
        this.message = message;
        this.validator = validator;
    }

    public String render() {
        return String.format("%s complained about %s : %s", validator.getSimpleName(), getPrefix(), message);
    }

    public Level getLevel() {
        return level;
    }

    public String getPrefix() {
        final String fn = (field != null) ? field.getJavaFieldName() : "";
        return clazz.getClazz().getName() + "." + fn;
    }
}