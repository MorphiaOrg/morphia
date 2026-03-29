package dev.morphia.rewrite.recipes;

import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.MethodCall;

/**
 * A {@link MethodMatcher} that matches against multiple delegate matchers, returning true if any one matches.
 */
public class MultiMethodMatcher extends MethodMatcher {
    private final MethodMatcher[] matchers;

    /**
     * Creates a MultiMethodMatcher that delegates to the given matchers.
     *
     * @param matchers the matchers to delegate to
     */
    public MultiMethodMatcher(MethodMatcher... matchers) {
        super("dev.morphia.Dummy dummy()", false);
        this.matchers = matchers;
    }

    @Override
    public boolean matches(MethodCall methodCall) {
        for (MethodMatcher matcher : matchers) {
            if (matcher.matches(methodCall)) {
                return true;
            }
        }
        return false;
    }
}
