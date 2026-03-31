package dev.morphia.critter.parser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator;
import dev.morphia.critter.sources.Example;

import org.bson.codecs.pojo.PropertyAccessor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static dev.morphia.critter.parser.GeneratorsTestHelper.defaultMapper;

public class TestVarHandleAccessor {
    private CritterClassLoader classLoader;

    @BeforeClass
    public void setup() {
        classLoader = new CritterClassLoader();
        new CritterGizmoGenerator(defaultMapper()).generate(Example.class, classLoader, true);
    }

    @Test
    public void testEntityNotModified() {
        List<String> methods = Arrays.stream(Example.class.getDeclaredMethods())
                .map(m -> m.getName())
                .collect(Collectors.toList());

        List<String> syntheticRead = methods.stream()
                .filter(name -> name.startsWith("__read") && !name.endsWith("Template"))
                .collect(Collectors.toList());
        List<String> syntheticWrite = methods.stream()
                .filter(name -> name.startsWith("__write") && !name.endsWith("Template"))
                .collect(Collectors.toList());

        Assert.assertTrue(syntheticRead.isEmpty(),
                "Entity class should not have synthetic __read methods but found: " + syntheticRead);
        Assert.assertTrue(syntheticWrite.isEmpty(),
                "Entity class should not have synthetic __write methods but found: " + syntheticWrite);
    }

    @Test
    public void testStringField() throws Exception {
        Example entity = new Example();
        PropertyAccessor<String> accessor = loadAccessor(Example.class, "name");

        Assert.assertNull(accessor.get(entity));
        accessor.set(entity, "hello");
        Assert.assertEquals(accessor.get(entity), "hello");
    }

    @Test
    public void testIntPrimitiveField() throws Exception {
        Example entity = new Example();
        PropertyAccessor<Object> accessor = loadAccessor(Example.class, "age");

        Assert.assertEquals(accessor.get(entity), 21);
        accessor.set(entity, 42);
        Assert.assertEquals(accessor.get(entity), 42);
    }

    @Test
    public void testLongBoxedField() throws Exception {
        Example entity = new Example();
        PropertyAccessor<Object> accessor = loadAccessor(Example.class, "salary");

        Assert.assertEquals(accessor.get(entity), 2L);
        accessor.set(entity, 100_000L);
        Assert.assertEquals(accessor.get(entity), 100_000L);
    }

    @Test
    public void testAccessorsInstantiatable() throws Exception {
        for (String field : List.of("name", "age", "salary")) {
            Class<?> cls = classLoader.loadClass(
                    Critter.critterPackage(Example.class) + "." + Critter.titleCase(field) + "Accessor");
            cls.getConstructor().newInstance();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> PropertyAccessor<T> loadAccessor(Class<?> entityType, String fieldName) throws Exception {
        Class<PropertyAccessor<T>> cls = (Class<PropertyAccessor<T>>) classLoader.loadClass(
                Critter.critterPackage(entityType) + "." + Critter.titleCase(fieldName) + "Accessor");
        return cls.getConstructor().newInstance();
    }
}
