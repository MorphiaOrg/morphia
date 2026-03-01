package dev.morphia.critter.parser;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.NoInjection;

public class TestPropertyModelGenerator extends BaseCritterTest {
    private final CritterClassLoader critterClassLoader = new CritterClassLoader();

    // @Test(dataProvider = "properties", testName = "")
    public void testProperty(String control, String methodName, @NoInjection Method method) throws Exception {
        CritterPropertyModel propertyModel = getModel(control);
        System.out.println("exampleModel = [" + control + "], methodName = [" + methodName + "], method = [" + method + "]");
        Object expected = method.invoke(control);
        Object actual = method.invoke(propertyModel);
        Assert.assertEquals(actual, expected, method.getName() + " should return the same value");
    }

    private CritterPropertyModel getModel(String name) {
        return (CritterPropertyModel) GeneratorTest.entityModel.getProperty(name);
    }

    @DataProvider(name = "properties")
    public Object[][] methods() {
        Object[][] methods = GeneratorTest.methodNames(CritterPropertyModel.class);
        return List.of("dev.morphia.critter.sources.ExampleNamePropertyModelTemplate").stream()
                .map(type -> {
                    try {
                        return loadModel(type);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(propertyModel -> Arrays.stream(methods)
                        .map(method -> new Object[] { propertyModel.getName(), method[0], method[1] }))
                .toArray(Object[][]::new);
    }

    private PropertyModel loadModel(String type) throws Exception {
        return (PropertyModel) critterClassLoader
                .loadClass(type)
                .getConstructor(EntityModel.class)
                .newInstance(GeneratorTest.entityModel);
    }
}
