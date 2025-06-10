package dev.morphia.rewrite.recipes;

import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.MethodCall;

public class MultiMethodMatcher extends MethodMatcher {
    private final MethodMatcher[] matchers;

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
