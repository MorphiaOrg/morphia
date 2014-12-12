package org.mongodb.morphia.testutil;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ExactClassMatcher extends TypeSafeMatcher<Class> {
    private final Class expectedClass;

    public ExactClassMatcher(final Class expectedClass) {
        this.expectedClass = expectedClass;
    }

    @Override
    protected boolean matchesSafely(final Class item) {
        return expectedClass == item;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendValue(expectedClass.getCanonicalName());
    }

    public static final ExactClassMatcher exactClass(final Class expectedValue) {
        return new ExactClassMatcher(expectedValue);
    }
}
