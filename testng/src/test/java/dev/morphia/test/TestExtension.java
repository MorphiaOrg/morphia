package dev.morphia.test;

public class TestExtension /*implements BeforeEachCallback*//*, TestInstancePostProcessor, AfterEachCallback*//* */ {
/*
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        System.out.println("TestExtension.beforeEach");
        Object testInstance = context.getTestInstance().get();
        testInstance.getClass().getDeclaredField("injected")
                    .set(testInstance, "Hello");
    }

    //    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        System.out.println("TestExtension.postProcessTestInstance");
        testInstance.getClass().getDeclaredField("injected")
                    .set(testInstance, "Hello");
    }
*/
}

