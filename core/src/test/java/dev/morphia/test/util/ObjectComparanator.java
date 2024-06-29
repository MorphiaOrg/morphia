package dev.morphia.test.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.bson.types.Decimal128;

import static dev.morphia.aggregation.expressions.MathExpressions.abs;
import static java.lang.Math.abs;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ObjectComparanator extends BaseComparanator {
    private final Object first;

    private final Object second;

    public ObjectComparanator(Comparanator parent, Object first, Object second, String message) {
        super(parent, message);
        if (first instanceof Number firstNumber && second instanceof Number secondNumber) {
            if (first instanceof Double || second instanceof Double) {
                this.first = firstNumber.doubleValue();
                this.second = secondNumber.doubleValue();
            } else {
                this.first = first;
                this.second = second;
            }
        } else {
            this.first = first;
            this.second = second;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean compare() {
        try {
            if (first instanceof Double firstNumber && second instanceof Double secondNumber) {
                assertTrue(abs(firstNumber - secondNumber) < 0.001, message);
            } else if (first instanceof Decimal128 firstComparable && second instanceof Decimal128 secondComparable) {
                assertEquals(firstComparable.compareTo(secondComparable), 0, message);
            } else {
                assertEquals(first, second, message);
            }
        } catch (AssertionError e) {
            Set<String> messages = new LinkedHashSet<>();
            messages.add(message);
            error(messages);
        }
        return true;
    }

}
