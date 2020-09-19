package dev.morphia.mapping;

import dev.morphia.TestBase;
import org.junit.Assert;
import org.junit.Test;

public class NamingStrategyTest extends TestBase {
    @Test
    public void apply() {
        compare("TestValue", "test_value", "testValue", "test-value");
    }

    private void compare(String input, String snake, String camel, String kebab) {
        Assert.assertEquals("Should be unchanged", input, NamingStrategy.identity().apply(input));
        Assert.assertEquals("Should be in snake case", snake, NamingStrategy.snakeCase().apply(input));
        Assert.assertEquals("Should be in camel case", camel, NamingStrategy.camelCase().apply(input));
        Assert.assertEquals("Should be in kebab case", kebab, NamingStrategy.kebabCase().apply(input));
    }
}