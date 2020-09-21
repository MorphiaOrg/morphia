package dev.morphia.test;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class SampleTest {
    String injected;

    @Test
    public void testExtension() {
        assertNotNull(injected);
        assertNull(injected);
    }

}
