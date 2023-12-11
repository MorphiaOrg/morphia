package dev.morphia.test.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.bson.assertions.Assertions.fail;

public class BaseComparanator implements Comparanator {
    protected List<Comparanator> comparanators = new ArrayList<>();
    private final Comparanator parent;
    protected final String message;

    private Set<String> errors = new LinkedHashSet<>();

    public BaseComparanator(Comparanator parent, String message) {
        this.parent = parent;
        this.message = message;
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

        if (parent == null) {
            check();
        }
        return true;
    }

    private boolean check() {
        if (!errors.isEmpty()) {
            StringJoiner joiner = new StringJoiner("\n\t", "\n\t", "");
            String collected = joiner.toString();
            if (!collected.isBlank()) {
                fail("""
                        %s
                        -----
                        collected errors:
                        %s
                        -----""".formatted(message, collected));
            }
        }
        return true;
    }

    @Override
    public void error(Set<String> messages) {
        if (parent != null) {
            messages.add(this.message);
            parent.error(messages);
        } else {
            //            messages.add(message);
            String collected = messages.stream().collect(Collectors.joining("\n  * ", "\n", ""));
            throw new AssertionError(collected);
        }
    }
}
