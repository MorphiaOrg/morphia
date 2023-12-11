package dev.morphia.test.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class ListComparanator extends BaseComparanator {

    public ListComparanator(Comparanator parent, List<?> actual, List<?> expected, boolean orderMatters) {
        super(parent, "Lists should match:\n\tactual: %s\n\texpected: %s".formatted(actual, expected));
        comparanators.add(new ObjectComparator(this, actual.size(), expected.size(),
                "List sizes should match:\n\tactual: %s\n\texpected: %s".formatted(actual, expected)));
        if (orderMatters) {
            orderedComparison(actual, expected);
        } else {
            unorderedComparison(actual, expected);
        }
    }

    private void orderedComparison(List<?> actual, List<?> expected) {
        Iterator<?> actualIterator = actual.iterator();
        Iterator<?> expectedIterator = expected.iterator();
        while (actualIterator.hasNext() && expectedIterator.hasNext()) {
            comparanators.add(Comparanator.of(this, actualIterator.next(), expectedIterator.next(), true));
        }
    }

    private void unorderedComparison(List<?> actual, List<?> expected) {
        actual.forEach(o -> comparanators.add(new ScanComparanator(this, o, expected, false)));
    }

    @Override
    public boolean compare() {
        for (Comparanator c : comparanators) {
            try {
                c.compare();
            } catch (AssertionError e) {
                Set<String> messages = new LinkedHashSet<>();
                messages.add(e.getMessage());
                error(messages);
            }
        }
        return true;
    }
}
