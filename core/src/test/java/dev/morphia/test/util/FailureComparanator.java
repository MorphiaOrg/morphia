package dev.morphia.test.util;

import static org.bson.assertions.Assertions.fail;

class FailureComparanator extends BaseComparanator {
    public FailureComparanator(Comparanator parent, String message) {
        super(parent, message);
    }

    @Override
    public boolean compare() {
        fail(message);
        return false;
    }
}
