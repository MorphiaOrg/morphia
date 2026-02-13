package dev.morphia.critter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Validates that the critter-maven plugin successfully generated entity models,
 * property models, and synthetic accessor methods for known entities.
 */
public class CritterGenerationTest {

    private static final String MORPHIA_PACKAGE = "__morphia";

    @DataProvider(name = "entities")
    public Object[][] entities() {
        return new Object[][] {
                { "dev.morphia.test.models.Hotel",
                        List.of("Name", "Stars", "Address", "StartDate", "Tags", "PhoneNumbers") },
                { "dev.morphia.test.models.User",
                        List.of("Name", "Age", "Id", "Joined", "Likes") },
        };
    }

    @Test(dataProvider = "entities")
    public void entityModelShouldBeGenerated(String entityClassName, List<String> expectedProperties) throws Exception {
        Class<?> entityClass = Class.forName(entityClassName);
        String entityModelClassName = entityClass.getPackageName()
                + "." + MORPHIA_PACKAGE
                + "." + entityClass.getSimpleName().toLowerCase()
                + "." + entityClass.getSimpleName() + "EntityModel";

        Class<?> entityModelClass = Class.forName(entityModelClassName);
        assertNotNull(entityModelClass, "EntityModel class should exist: " + entityModelClassName);
        assertTrue(CritterEntityModel.class.isAssignableFrom(entityModelClass),
                entityModelClassName + " should extend CritterEntityModel");
    }

    @Test(dataProvider = "entities")
    public void propertyModelsShouldBeGenerated(String entityClassName, List<String> expectedProperties)
            throws Exception {
        Class<?> entityClass = Class.forName(entityClassName);
        String basePackage = entityClass.getPackageName()
                + "." + MORPHIA_PACKAGE
                + "." + entityClass.getSimpleName().toLowerCase();

        for (String property : expectedProperties) {
            String modelClassName = basePackage + "." + property + "Model";
            Class<?> modelClass = Class.forName(modelClassName);
            assertNotNull(modelClass, "Property model should exist: " + modelClassName);
        }
    }

    @Test(dataProvider = "entities")
    public void syntheticAccessorMethodsShouldExist(String entityClassName, List<String> expectedProperties)
            throws Exception {
        Class<?> entityClass = Class.forName(entityClassName);
        Method[] methods = entityClass.getDeclaredMethods();
        List<String> methodNames = Arrays.stream(methods).map(Method::getName).toList();

        for (String property : expectedProperties) {
            String readMethod = "__read" + property;
            assertTrue(methodNames.contains(readMethod),
                    entityClass.getSimpleName() + " should have synthetic method " + readMethod
                            + ". Found methods: " + methodNames);
        }
    }
}
