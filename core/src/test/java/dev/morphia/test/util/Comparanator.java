package dev.morphia.test.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.test.TestBase.coerceToLong;

public interface Comparanator {
    static Comparanator of(Comparanator parent, Object actual, Object expected, boolean orderMatters) {
        if (actual instanceof List && expected instanceof List) {
            return new ListComparanator(null, (List<?>) actual, (List<?>) expected, orderMatters);
        } else if (actual instanceof Map && expected instanceof Map) {
            return new MapComparanator(parent, (Map<?, ?>) actual, (Map<?, ?>) expected, orderMatters);
            //        } else if (actual instanceof Double && expected instanceof Double) {
            //            return new ObjectComparator(parent, actual.toString(), expected.toString(), "values should match");
        } else {
            var message = "values should match:  '%s'  ---  '%s'".formatted(actual, expected);
            if (actual instanceof Integer && expected instanceof Integer) {
                return new ObjectComparator(parent, coerceToLong(actual), coerceToLong(expected), message);
            } else {
                return new ObjectComparator(parent, actual, expected, message);
            }
        }
    }

    boolean compare();

    void error(Set<String> messages);
}