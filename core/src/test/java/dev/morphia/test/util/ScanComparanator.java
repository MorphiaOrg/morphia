package dev.morphia.test.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanComparanator extends BaseComparanator {
    List<Comparanator> comparanators = new ArrayList<>();

    public ScanComparanator(Comparanator parent, Object value, List<?> list, boolean orderMatters) {
        super(parent, "Should find \n\t'%s'\n\nin\n\t%s".formatted(value, list));
        list.forEach(item -> {
            comparanators.add(Comparanator.of(this, value, item, orderMatters));
        });
    }

    @Override
    public boolean compare() {
        var found = new AtomicBoolean(false);
        comparanators.forEach(comparanator -> {
            try {
                comparanator.compare();
                found.set(true);
            } catch (AssertionError e) {
            }
        });

        if (!found.get()) {
            Set<String> messages = new LinkedHashSet<>();
            messages.add(message);
            error(messages);
        }
        return true;
    }
}
