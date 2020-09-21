package dev.morphia.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSample {
    String injected;

    @Test
    public void testExtension() {
        Assertions.assertNotNull(injected);
        Assertions.assertNull(injected);
    }

}
