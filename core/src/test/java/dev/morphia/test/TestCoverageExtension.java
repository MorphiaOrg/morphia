package dev.morphia.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Fails an operator example class whose tests or fixture directories disagree with its accepted bindings.
 *
 * @see ExampleBindings
 */
public class TestCoverageExtension implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) {
        ExampleBindings.verify(context.getRequiredTestClass());
    }
}
