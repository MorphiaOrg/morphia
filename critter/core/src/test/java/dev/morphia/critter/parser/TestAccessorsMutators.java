package dev.morphia.critter.parser;

import java.util.List;

import dev.morphia.critter.Critter;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.CritterKt;
import dev.morphia.critter.sources.Example;

import org.bson.codecs.pojo.PropertyAccessor;
import org.testng.Assert;
import org.testng.annotations.DataProvider;

public class TestAccessorsMutators extends BaseCritterTest {
    private final CritterClassLoader critterClassLoader = new CritterClassLoader();

    // @Test(dataProvider = "classes")
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
                Critter.Companion.critterPackage(type)
                        + type.getSimpleName()
                        + CritterKt.titleCase(fieldName)
                        + "Accessor");
        PropertyAccessor<Object> accessor = accessorClass.getConstructor().newInstance();

        accessor.set(entity, testValue);
        Assert.assertEquals(accessor.get(entity), testValue);
        Assert.assertTrue(
                entity.toString().contains(testValue.toString()),
                "Could not find '" + testValue + "` in :" + entity);
    }

    @DataProvider(name = "classes")
    public Object[][] names() {
        return new Object[][] { { Example.class } };
    }
}
