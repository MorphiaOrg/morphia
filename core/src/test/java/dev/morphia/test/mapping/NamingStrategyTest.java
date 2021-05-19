package dev.morphia.test.mapping;

import dev.morphia.mapping.NamingStrategy;
import dev.morphia.test.TestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

public class NamingStrategyTest extends TestBase {
    @Test
    public void apply() {
        Assert.assertEquals(NamingStrategy.identity().apply("TestValue"), "TestValue", "Should be unchanged");
        Assert.assertEquals(NamingStrategy.snakeCase().apply("TestValue"), "test_value", "Should be in snake case");
        Assert.assertEquals(NamingStrategy.camelCase().apply("TestValue"), "testValue", "Should be in camel case");
        Assert.assertEquals(NamingStrategy.kebabCase().apply("TestValue"), "test-value", "Should be in kebab case");
        Assert.assertEquals(NamingStrategy.lowerCase().apply("TestValue"), "testvalue", "Should be in lower case");
    }
}