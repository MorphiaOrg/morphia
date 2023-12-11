package dev.morphia.test.util;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class ObjectComparator extends BaseComparanator {
    private final Object first;

    private final Object second;

    public ObjectComparator(Comparanator parent, Object first, Object second, String message) {
        super(parent, message);
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean compare() {
        try {
            assertEquals(first, second, message);
        } catch (AssertionError e) {
            Set<String> messages = new LinkedHashSet<>();
            messages.add(message);
            error(messages);
        }
        return true;
    }

}
