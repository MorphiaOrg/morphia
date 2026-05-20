package dev.morphia.critter.parser;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.critter.CritterPropertyModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestPropertyModelGenerator extends BaseCritterTest {
    private final CritterClassLoader critterClassLoader = new CritterClassLoader();

    // @ParameterizedTest
    @MethodSource("properties")
    @DisplayName("")
    public void testProperty(String control, String methodName, Method method) throws Exception {
        CritterPropertyModel propertyModel = getModel(control);
        System.out.println("exampleModel = [" + control + "], methodName = [" + methodName + "], method = [" + method + "]");
        Object expected = method.invoke(control);
        Object actual = method.invoke(propertyModel);
        Assertions.assertEquals(expected, actual, method.getName() + " should return the same value");
    }

    private CritterPropertyModel getModel(String name) {
        return (CritterPropertyModel) GeneratorTest.entityModel.getProperty(name);
    }

    static Stream<Arguments> properties() {
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
                        .map(method -> Arguments.of(propertyModel.getName(), method[0], method[1])));
    }

    private static PropertyModel loadModel(String type) throws Exception {
        return (PropertyModel) new CritterClassLoader()
                .loadClass(type)
                .getConstructor(EntityModel.class)
                .newInstance(GeneratorTest.entityModel);
    }
}
