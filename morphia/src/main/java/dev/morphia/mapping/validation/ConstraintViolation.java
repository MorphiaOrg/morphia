package dev.morphia.mapping.validation;


import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ConstraintViolation {
    private final MappedClass clazz;
    private final Class<? extends ClassConstraint> validator;
    private final String message;
    private final Level level;
    private MappedField field;

    /**
     * Creates a violation instance to record invalid mapping metadata
     *
     * @param level     the severity of the violation
     * @param clazz     the errant class
     * @param field     the errant field
     * @param validator the constraint failed
     * @param message   the message for the failure
     */
    public ConstraintViolation(final Level level, final MappedClass clazz, final MappedField field,
                               final Class<? extends ClassConstraint> validator, final String message) {
        this(level, clazz, validator, message);
        this.field = field;
    }

    /**
     * Creates a violation instance to record invalid mapping metadata
     *
     * @param level     the severity of the violation
     * @param clazz     the errant class
     * @param validator the constraint failed
     * @param message   the message for the failure
     */
    public ConstraintViolation(final Level level, final MappedClass clazz, final Class<? extends ClassConstraint> validator,
                               final String message) {
        this.level = level;
        this.clazz = clazz;
        this.message = message;
        this.validator = validator;
    }

    /**
     * @return the severity of the violation
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return the qualified name of the failing mapping
     */
    public String getPrefix() {
        final String fn = (field != null) ? field.getJavaFieldName() : "";
        return clazz.getClazz().getName() + "." + fn;
    }

    /**
     * @return a human friendly version of the violation
     */
    public String render() {
        return String.format("%s complained about %s : %s", validator.getSimpleName(), getPrefix(), message);
    }

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
}
