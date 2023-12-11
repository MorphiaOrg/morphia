package dev.morphia.test.util;

import java.util.Map;

public class MapComparanator extends BaseComparanator {
    private final Map<?, ?> actual;

    private final Map<?, ?> expected;

    private final boolean orderMatters;

    public MapComparanator(Comparanator parent, Map<?, ?> actual, Map<?, ?> expected, boolean orderMatters) {
        super(parent, "Maps should match:\n\t%s\n\t%s".formatted(actual, expected));
        this.actual = actual;
        this.expected = expected;
        this.orderMatters = orderMatters;
        actual.forEach((key, value) -> {
            if (!expected.containsKey(key)) {
                comparanators.add(new FailureComparanator(this, "Could not find the key '%s' in %s".formatted(key, expected)));
            } else {
                comparanators.add(Comparanator.of(this, value, expected.get(key), orderMatters));
            }
        });
    }
}
