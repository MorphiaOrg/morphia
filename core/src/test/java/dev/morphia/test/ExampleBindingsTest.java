package dev.morphia.test;

import java.util.List;

import dev.morphia.test.ExampleBindings.Binding;
import dev.morphia.test.ExampleBindings.Descriptor;
import dev.morphia.test.ExampleBindings.Manual;
import dev.morphia.test.ExampleBindings.TestClass;
import dev.morphia.test.ExampleBindings.Unbound;
import dev.morphia.test.aggregation.expressions.TestAbs;
import dev.morphia.test.query.FiltersTest;

import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline completeness check: runs without MongoDB and also catches a removed class or a class whose last test was removed,
 * which a per-class AfterAll callback would never see.
 */
public class ExampleBindingsTest {
    private static final List<String> PACKAGES = List.copyOf(ExampleBindings.PACKAGES);

    @Test
    public void everyOperatorClassAndFixtureIsBound() {
        ExampleBindings.verifyAll();
    }

    @Test
    public void resourcesComeFromTheDescriptor() throws NoSuchMethodException {
        assertEquals("absExpressionOperator",
                ExampleBindings.resource(TestAbs.class.getDeclaredMethod("testAbsExpressionOperator")));
        assertNull(ExampleBindings.lookup(FiltersTest.class));
    }

    @Test
    public void rejectsInvalidDescriptors() {
        var abs = entry(of(new Binding("testExample1", "example1")), of(), of());
        ExampleBindings.validate(descriptor(of(abs)));

        rejects(new Descriptor(2, PACKAGES, of(abs)), "schema");
        rejects(new Descriptor(1, PACKAGES, of()), "No example binding classes");
        rejects(descriptor(of(abs, abs)), "Duplicate binding class");
        rejects(new Descriptor(1, of("dev.morphia.test.aggregation.expressions"), of(abs)), "must be exactly");
        rejects(descriptor(of(new TestClass(FiltersTest.class.getName(), "dev/morphia/test/query/filters", of(), of(),
                of()))), "outside the declared packages");
        rejects(descriptor(of(new TestClass(TestAbs.class.getName(), "dev/morphia/test/aggregation/expressions/other",
                of(), of(), of()))), "does not match");
        rejects(descriptor(of(entry(of(new Binding("testExample1", "example1"), new Binding("testExample1", "example2")),
                of(), of()))), "Duplicate method owner");
        rejects(descriptor(of(entry(of(new Binding("testExample1", "example1"), new Binding("testExample2", "Example1")),
                of(), of()))), "Duplicate fixture owner");
        rejects(descriptor(of(entry(of(new Binding("testExample1", "example1")), of(),
                of(new Unbound("example1", "watch example"))))), "Duplicate fixture owner");
        rejects(descriptor(of(entry(of(), of(new Manual("testOther", " ")), of()))), "need a reason");
        rejects(descriptor(of(entry(of(new Binding("testExample1", "../example1")), of(), of()))), "Invalid fixture");
    }

    private static TestClass entry(List<Binding> bindings, List<Manual> manual, List<Unbound> unbound) {
        return new TestClass(TestAbs.class.getName(), "dev/morphia/test/aggregation/expressions/abs", bindings, manual, unbound);
    }

    private static Descriptor descriptor(List<TestClass> classes) {
        return new Descriptor(1, PACKAGES, classes);
    }

    private static void rejects(Descriptor descriptor, String message) {
        var e = assertThrows(IllegalStateException.class, () -> ExampleBindings.validate(descriptor));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }
}
