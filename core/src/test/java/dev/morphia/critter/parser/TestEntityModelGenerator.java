package dev.morphia.critter.parser;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import dev.morphia.critter.ClassfileOutput;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestEntityModelGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(TestEntityModelGenerator.class);

    private final CritterEntityModel control;
    private final Mapper mapper = GeneratorsTestHelper.defaultMapper();
    private final CritterClassLoader critterClassLoader = new CritterClassLoader();

    public TestEntityModelGenerator() {
        CritterEntityModel tmp;
        try {
            tmp = (CritterEntityModel) critterClassLoader
                    .loadClass("dev.morphia.critter.sources.ExampleEntityModelTemplate")
                    .getConstructor(Mapper.class)
                    .newInstance(mapper);
            ClassfileOutput.dump(critterClassLoader, "dev.morphia.critter.sources.ExampleEntityModelTemplate");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        control = tmp;
    }

    // @ParameterizedTest
    @MethodSource("methods")
    public void testEntityModel(String name, Method method) throws Exception {
        Object expected = method.invoke(control);
        Object actual = method.invoke(GeneratorTest.entityModel);
        Assertions.assertEquals(expected, actual, method.getName() + " should return the same value");
    }

    static Stream<Arguments> methods() {
        return Arrays.stream(GeneratorTest.methodNames(CritterEntityModel.class))
                .map(row -> Arguments.of(row));
    }
}
