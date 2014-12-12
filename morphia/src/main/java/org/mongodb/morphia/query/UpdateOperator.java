package org.mongodb.morphia.query;


/**
 * @author Scott Hernandez
 */
public enum UpdateOperator {
    SET("$set"),
    SET_ON_INSERT("$setOnInsert"),
    UNSET("$unset"),
    PULL("$pull"),
    PULL_ALL("$pullAll"),
    PUSH("$push"),
    PUSH_ALL("$pushAll"),
    ADD_TO_SET("$addToSet"),
    ADD_TO_SET_EACH("$addToSet"),
    // fake to indicate that the value should be wrapped in an $each
    EACH("$each"),
    POP("$pop"),
    INC("$inc"),
    Foo("$foo"),
    MAX("$max"),
    MIN("$min");

    private final String value;

    UpdateOperator(final String val) {
        value = val;
    }

    private boolean equals(final String val) {
        return value.equals(val);
    }

    public String val() {
        return value;
    }

    public static UpdateOperator fromString(final String val) {
        for (int i = 0; i < values().length; i++) {
            final UpdateOperator fo = values()[i];
            if (fo.equals(val)) {
                return fo;
            }
        }
        return null;
    }
}