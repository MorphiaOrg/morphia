package dev.morphia.utils;

import static java.lang.String.format;

/**
 * Provides various assertions for Morphia during validation
 */
public final class Assert {
    private Assert() {
    }

    /**
     * Throws an AssertionFailedException with the given error message.
     *
     * @param error the error message
     */
    public static void raiseError(final String error) {
        throw new AssertionFailedException(error);
    }

    /**
     * Validates that all the parameters are not null
     *
     * @param names   a comma separated String of parameter names
     * @param objects the proposed parameter values
     */
    public static void parametersNotNull(final String names, final Object... objects) {
        String msgPrefix = "At least one of the parameters";

        if (objects != null) {
            if (objects.length == 1) {
                msgPrefix = "Parameter";
            }

            for (final Object object : objects) {
                if (object == null) {
                    raiseError(String.format("%s '%s' is null.", msgPrefix, names));
                }
            }
        }
    }

    /**
     * Validates that the parameter is not null
     *
     * @param name      the parameter name
     * @param reference the proposed parameter value
     */
    public static void parameterNotNull(final String name, final Object reference) {
        if (reference == null) {
            raiseError(format("Parameter '%s' is not expected to be null.", name));
        }
    }

    /**
     * Validates that the Iterable is not empty
     *
     * @param name the parameter name
     * @param obj  the proposed parameter value
     */
    public static void parameterNotEmpty(final String name, final Iterable obj) {
        if (!obj.iterator().hasNext()) {
            raiseError(format("Parameter '%s' from type '%s' is expected to NOT be empty", name, obj.getClass().getName()));
        }
    }

    /**
     * Validates that the value is not empty
     *
     * @param name  the parameter name
     * @param value the proposed parameter value
     */
    public static void parameterNotEmpty(final String name, final String value) {
        if (value != null && value.length() == 0) {
            raiseError(format("Parameter '%s' is expected to NOT be empty.", name));
        }
    }

    /**
     * Represents a failed Morphia Assertion
     */
    public static class AssertionFailedException extends RuntimeException {

        private static final long serialVersionUID = 435272532743543854L;

        /**
         * Creates a AssertionFailedException with a message and a cause
         *
         * @param message the message to record
         */
        public AssertionFailedException(final String message) {
            super(message);
        }

        /**
         * Creates a AssertionFailedException with a message and a cause
         *
         * @param message the message to record
         * @param cause   the underlying cause
         */
        public AssertionFailedException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
