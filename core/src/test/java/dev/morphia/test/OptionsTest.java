package dev.morphia.test;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import dev.morphia.DeleteOptions;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.query.CountOptions;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public class OptionsTest {
    @Test
    public void aggregationOptions() {
        scan(com.mongodb.AggregationOptions.class, AggregationOptions.class, false, List.of(ReadConcern.class, ReadPreference.class,
            WriteConcern.class));
    }

    @Test
    public void countOptions() {
        scan(com.mongodb.client.model.CountOptions.class, CountOptions.class, true, List.of(ReadConcern.class, ReadPreference.class));
    }

    @Test
    public void deleteOptions() {
        scan(com.mongodb.client.model.DeleteOptions.class, DeleteOptions.class, true, List.of(WriteConcern.class));
    }

    @Test
    public void findAndDeleteOptions() {
        scan(FindOneAndDeleteOptions.class, FindAndDeleteOptions.class, true, List.of(WriteConcern.class));
    }

    @Test
    public void findAndModifyOptions() {
        scan(FindOneAndUpdateOptions.class, ModifyOptions.class, true, List.of(WriteConcern.class));
    }

    @Test
    public void findOptions() {
        beanScan(FindIterable.class, FindOptions.class, List.of("explain", "filter", "projection"));
    }

    @Test
    public void insertManyOptions() {
        scan(com.mongodb.client.model.InsertManyOptions.class, InsertManyOptions.class, false, List.of(WriteConcern.class));
    }

    @Test
    public void insertOneOptions() {
        scan(com.mongodb.client.model.InsertOneOptions.class, InsertOneOptions.class, false, List.of(WriteConcern.class));
    }

    @Test
    public void updateOptions() {
        scan(com.mongodb.client.model.UpdateOptions.class, UpdateOptions.class, true, List.of(WriteConcern.class));
    }

    private void beanScan(Class<?> driver, Class<?> morphia, List<String> filtered) {
        try {
            Method[] methods = driver.getDeclaredMethods();
            for (Method method : methods) {
                if (!filtered.contains(method.getName()) && method.getAnnotation(Deprecated.class) == null) {
                    morphia.getDeclaredMethod(method.getName(), convert(method.getParameterTypes()));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void checkOverride(Class<?> driverType, Class<?> morphiaType, Method method) throws NoSuchMethodException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Method morphiaMethod = morphiaType.getMethod(method.getName(), parameterTypes);
        Assert.assertTrue(!method.getReturnType().equals(driverType)
                          || morphiaMethod.getReturnType().equals(morphiaType), method.toString());

        if (parameterTypes.equals(new Class[]{Bson.class})) {
            Assert.assertTrue(!method.getReturnType().equals(driverType)
                              || morphiaType.getMethod(method.getName(), Document.class)
                                            .getReturnType().equals(morphiaType), method.toString());

        }
    }

    private Class<?>[] convert(Class<?>[] types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(Bson.class)) {
                types[i] = Document.class;
            }
        }
        return types;
    }

    private void scan(Class<?> driverType, Class<?> morphiaType, boolean subclass, List<Class<?>> localFields) {
        try {
            Method[] methods = driverType.getDeclaredMethods();
            Assert.assertEquals(driverType.equals(morphiaType.getSuperclass()), subclass, "Options class should be a subclass");
            for (Method method : methods) {
                if (method.getAnnotation(Deprecated.class) == null && !method.getName().equals("builder")) {
                    checkOverride(driverType, morphiaType, method);
                }
            }
            for (Class<?> localField : localFields) {
                String name = localField.getSimpleName()
                                        .replaceAll("^get", "");
                name = name.substring(0, 1).toLowerCase() + name.substring(1);

                Field field = morphiaType.getDeclaredField(name);
                Assert.assertEquals(localField, field.getType(), localField.getName());

                Method declaredMethod = morphiaType.getDeclaredMethod(name);
                Assert.assertEquals(localField, declaredMethod.getReturnType(), declaredMethod.toString());

                declaredMethod = morphiaType.getDeclaredMethod(name, localField);
                Assert.assertEquals(morphiaType, declaredMethod.getReturnType(), declaredMethod.toString());
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
