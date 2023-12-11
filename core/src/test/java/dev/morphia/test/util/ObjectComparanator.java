package dev.morphia.test.util;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class ObjectComparanator extends BaseComparanator {
    private final Object first;

    private final Object second;

    public ObjectComparanator(Comparanator parent, Object first, Object second, String message) {
        super(parent, message);
        if (first instanceof Number firstNumber && second instanceof Number secondNumber) {
            if (first instanceof Double && !(second instanceof Double)) {
                this.first = firstNumber;
                this.second = secondNumber.doubleValue();
            } else if (!(first instanceof Double) && second instanceof Double) {
                this.first = firstNumber.doubleValue();
                this.second = secondNumber;
            } else {
                this.first = first;
                this.second = second;
            }
        } else {
            this.first = first;
            this.second = second;
        }
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
