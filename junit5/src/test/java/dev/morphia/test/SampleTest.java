package dev.morphia.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestExtension.class)
public class SampleTest {
    String injected;

    @Test
    public void testExtension() {
        Assertions.assertNotNull(injected);
        Assertions.assertNull(injected);
    }

}
