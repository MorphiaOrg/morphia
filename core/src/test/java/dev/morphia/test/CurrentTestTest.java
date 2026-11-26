package dev.morphia.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(CurrentTest.class)
public class CurrentTestTest {
    @Test
    public void aPlainTestOwnsItsFixture() {
        assertEquals("aPlainTestOwnsItsFixture", CurrentTest.method().getName());
    }

    @Test
    public void aHelperOrLambdaSeesTheRunningTest() {
        Runnable lambda = () -> assertEquals("aHelperOrLambdaSeesTheRunningTest", CurrentTest.method().getName());
        lambda.run();
    }

    @ParameterizedTest
    @ValueSource(ints = { 1 })
    public void aParameterizedTestCannotOwnAFixture(int value) {
        IllegalStateException error = assertThrows(IllegalStateException.class, CurrentTest::method);
        assertTrue(error.getMessage().contains("not a plain @Test method"), error.getMessage());
    }

    @Test
    public void nothingIsRunningOnAnotherThread() throws InterruptedException {
        Throwable[] error = new Throwable[1];
        Thread thread = new Thread(() -> {
            try {
                CurrentTest.method();
            } catch (IllegalStateException e) {
                error[0] = e;
            }
        });
        thread.start();
        thread.join();
        assertTrue(error[0] != null && error[0].getMessage().contains("No test is running"));
    }
}
