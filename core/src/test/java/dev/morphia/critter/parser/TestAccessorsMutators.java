package dev.morphia.critter.parser;

import java.util.List;
import java.util.stream.Stream;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.sources.Example;

import org.bson.codecs.pojo.PropertyAccessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestAccessorsMutators extends BaseCritterTest {
    private final CritterClassLoader critterClassLoader = new CritterClassLoader();

    // @ParameterizedTest
    @MethodSource("classes")
    public void testPropertyAccessors(Class<?> type) throws Exception {
        List<List<Object>> testFields = List.of(
                List.of("name", String.class, "set externally"),
                List.of("age", int.class, 100),
                List.of("salary", Long.class, 100_000L));

        Object entity = critterClassLoader.loadClass(type.getName()).getConstructor().newInstance();

        for (List<Object> field : testFields) {
            testAccessor(type, critterClassLoader, entity, (String) field.get(0), field.get(2));
        }
    }

    @SuppressWarnings("unchecked")
    private void testAccessor(
            Class<?> type,
            CritterClassLoader loader,
            Object entity,
            String fieldName,
            Object testValue) throws Exception {
        Class<PropertyAccessor<Object>> accessorClass = (Class<PropertyAccessor<Object>>) loader.loadClass(
                Critter.critterPackage(type)
                        + type.getSimpleName()
                        + Critter.titleCase(fieldName)
                        + "Accessor");
        PropertyAccessor<Object> accessor = accessorClass.getConstructor().newInstance();

        accessor.set(entity, testValue);
        Assertions.assertEquals(testValue, accessor.get(entity));
        Assertions.assertTrue(
                entity.toString().contains(testValue.toString()),
                "Could not find '" + testValue + "` in :" + entity);
    }

    static Stream<Arguments> classes() {
        return Stream.of(Arguments.of(Example.class));
    }
}
