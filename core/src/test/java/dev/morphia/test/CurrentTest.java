package dev.morphia.test;

import java.lang.reflect.Method;

import com.mongodb.lang.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Records the test method JUnit is running, so a test's fixture is found from its method name without walking the
 * stack. Only a plain {@code @Test} method with no parameters can own a fixture; a parameterized, repeated or factory
 * test, or one with injected parameters, is reported when it asks for its fixture.
 */
public class CurrentTest implements BeforeEachCallback, AfterEachCallback {
    private static final ThreadLocal<Method> METHOD = new ThreadLocal<>();

    @Override
    public void beforeEach(ExtensionContext context) {
        METHOD.set(context.getRequiredTestMethod());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        METHOD.remove();
    }

    /**
     * @return the running test method, which must be able to own a fixture
     * @throws IllegalStateException when no test is running, or the running test cannot own a fixture
     */
    public static Method method() {
        Method method = METHOD.get();
        if (method == null) {
            throw new IllegalStateException("No test is running on this thread: a fixture is found from the running test");
        }
        String problem = problem(method);
        if (problem != null) {
            throw new IllegalStateException(method.getDeclaringClass().getName() + "#" + method.getName() + " " + problem);
        }
        return method;
    }

    /**
     * @param method a test method
     * @return why it cannot own a fixture, or null when it can
     */
    @Nullable
    static String problem(Method method) {
        if (method.getAnnotation(Test.class) == null) {
            return "is not a plain @Test method, so it cannot own a fixture";
        }
        if (method.getParameterCount() != 0) {
            return "takes parameters, so it cannot own a fixture";
        }
        return null;
    }
}
