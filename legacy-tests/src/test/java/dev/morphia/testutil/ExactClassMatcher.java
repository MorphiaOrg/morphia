package dev.morphia.testutil;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ExactClassMatcher extends TypeSafeMatcher<Class> {
    private final Class expectedClass;

    public ExactClassMatcher(final Class expectedClass) {
        this.expectedClass = expectedClass;
    }

    public static ExactClassMatcher exactClass(final Class expectedValue) {
        return new ExactClassMatcher(expectedValue);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendValue(expectedClass.getCanonicalName());
    }

    @Override
    protected boolean matchesSafely(final Class item) {
        return expectedClass == item;
    }
}
