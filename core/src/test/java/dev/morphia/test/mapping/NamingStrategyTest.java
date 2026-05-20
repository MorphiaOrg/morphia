package dev.morphia.test.mapping;

import dev.morphia.mapping.NamingStrategy;
import dev.morphia.test.TestBase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NamingStrategyTest extends TestBase {
    @Test
    public void apply() {
        Assertions.assertEquals("TestValue", NamingStrategy.identity().apply("TestValue"), "Should be unchanged");
        Assertions.assertEquals("test_value", NamingStrategy.snakeCase().apply("TestValue"), "Should be in snake case");
        Assertions.assertEquals("testValue", NamingStrategy.camelCase().apply("TestValue"), "Should be in camel case");
        Assertions.assertEquals("test-value", NamingStrategy.kebabCase().apply("TestValue"), "Should be in kebab case");
        Assertions.assertEquals("testvalue", NamingStrategy.lowerCase().apply("TestValue"), "Should be in lower case");
    }
}