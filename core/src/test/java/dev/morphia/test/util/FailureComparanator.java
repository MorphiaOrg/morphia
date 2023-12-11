package dev.morphia.test.util;

import java.util.LinkedHashSet;

class FailureComparanator extends BaseComparanator {
    public FailureComparanator(Comparanator parent, String message) {
        super(parent, message);
    }

    @Override
    public boolean compare() {
        var messages = new LinkedHashSet<String>();
        messages.add(message);
        error(messages);
        return false;
    }
}
