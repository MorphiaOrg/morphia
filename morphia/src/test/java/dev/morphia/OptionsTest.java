package dev.morphia;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import dev.morphia.query.CountOptions;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class OptionsTest {
    @Test
    public void findOptions() throws ReflectiveOperationException {
        beanScan(FindIterable.class, FindOptions.class, List.of("filter", "projection"));
    }

    void beanScan(final Class driver, final Class morphia, final List<String> filtered) throws ReflectiveOperationException {
        Method[] methods = driver.getDeclaredMethods();
        for (final Method method : methods) {
            if (!filtered.contains(method.getName()) && method.getAnnotation(Deprecated.class) == null) {
                morphia.getDeclaredMethod(method.getName(), convert(method.getParameterTypes()));
            }
        }
    }

    private Class<?>[] convert(final Class<?>[] types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(Bson.class)) {
                types[i] = Document.class;
            }
        }
        return types;
    }

    @Test
    public void countOptions() throws ReflectiveOperationException {
        scan(com.mongodb.client.model.CountOptions.class, CountOptions.class, true, List.of(ReadConcern.class, ReadPreference.class));
    }

    @Test
    public void findAndDeleteOptions() throws ReflectiveOperationException {
        scan(FindOneAndDeleteOptions.class, FindAndDeleteOptions.class, true, List.of(WriteConcern.class));
    }

    @Test
    public void findAndModifyOptions() throws ReflectiveOperationException {
        scan(FindOneAndUpdateOptions.class, FindAndModifyOptions.class, true, List.of(WriteConcern.class));
    }

    @Test
    public void insertOneOptions() throws ReflectiveOperationException {
        scan(com.mongodb.client.model.InsertOneOptions.class, InsertOneOptions.class, false, List.of(WriteConcern.class));
    }

    @Test
    public void insertManyOptions() throws ReflectiveOperationException {
        scan(com.mongodb.client.model.InsertManyOptions.class, InsertManyOptions.class, false, List.of(WriteConcern.class));
    }

    @Test
    public void updateOptions() throws ReflectiveOperationException {
        scan(com.mongodb.client.model.UpdateOptions.class, UpdateOptions.class, true, List.of(WriteConcern.class));
    }

    @Test
    public void deleteOptions() throws ReflectiveOperationException {
        scan(com.mongodb.client.model.DeleteOptions.class, DeleteOptions.class, true, List.of(WriteConcern.class));
    }

    void scan(final Class<?> driver, final Class morphia, final boolean subclass, List<Class> localFields) throws ReflectiveOperationException {
        Method[] methods = driver.getDeclaredMethods();
        Assert.assertEquals("Unexpected subclassing", subclass, driver.equals(morphia.getSuperclass()));
        for (final Method method : methods) {
            if (method.getAnnotation(Deprecated.class) == null) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                Assert.assertTrue(method.toString(), !method.getReturnType().equals(driver)
                                                     || morphia.getMethod(method.getName(), parameterTypes)
                                                                     .getReturnType().equals(morphia));

                if(parameterTypes.equals(new Class[] { Bson.class })) {
                    Assert.assertTrue(method.toString(), !method.getReturnType().equals(driver)
                                                         || morphia.getMethod(method.getName(), Document.class)
                                                                   .getReturnType().equals(morphia));

                }
            }
        }
        for (final Class localField : localFields) {
            String name = localField.getSimpleName()
                                .replaceAll("^get", "");
            name = name.substring(0, 1).toLowerCase() + name.substring(1);

            Field field = morphia.getDeclaredField(name);
            Assert.assertTrue(localField.getName(), field.getType().equals(localField));

            Method declaredMethod = morphia.getDeclaredMethod(name);
            Assert.assertTrue(declaredMethod.toString(), declaredMethod.getReturnType().equals(localField));

            declaredMethod = morphia.getDeclaredMethod(name, localField);
            Assert.assertTrue(declaredMethod.toString(), declaredMethod.getReturnType().equals(morphia));
        }
    }
}
