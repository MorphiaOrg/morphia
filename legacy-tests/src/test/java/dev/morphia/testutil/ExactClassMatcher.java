package dev.morphia.testutil;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ExactClassMatcher extends TypeSafeMatcher<Class> {
    private final Class expectedClass;

    public ExactClassMatcher(Class expectedClass) {
        this.expectedClass = expectedClass;
    }

    public static ExactClassMatcher exactClass(Class expectedValue) {
        return new ExactClassMatcher(expectedValue);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expectedClass.getCanonicalName());
    }

    @Override
    protected boolean matchesSafely(Class item) {
        return expectedClass == item;
    }
}
