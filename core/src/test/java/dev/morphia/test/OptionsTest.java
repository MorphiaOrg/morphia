package dev.morphia.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;

import dev.morphia.DeleteOptions;
import dev.morphia.InsertManyOptions;
import dev.morphia.InsertOneOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.ReplaceOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.query.CountOptions;
import dev.morphia.query.FindAndDeleteOptions;
import dev.morphia.query.FindOptions;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings("SameParameterValue")
public class OptionsTest extends TestBase {
    @Test
    public void aggregationOptions() {
        checkMinDriverVersion(DriverVersion.v52);
        beanScan(AggregateIterable.class, AggregationOptions.class, List.of("builder",
                "explain",
                "getAllowDiskUse",
                "getBatchSize",
                "getBypassDocumentValidation",
                "getCollation",
                "getMaxTime",
                "hintString",
                "toCollection"));
    }

    @Test
    public void countOptions() {
        scan(com.mongodb.client.model.CountOptions.class, CountOptions.class, List.of(ReadConcern.class, ReadPreference.class));
    }

    @Test
    public void deleteOptions() {
        scan(com.mongodb.client.model.DeleteOptions.class, DeleteOptions.class, List.of(WriteConcern.class));
    }

    @Test
    public void findAndDeleteOptions() {
        scan(FindOneAndDeleteOptions.class, FindAndDeleteOptions.class, List.of(WriteConcern.class));
    }

    @Test
    public void findAndModifyOptions() {
        scan(FindOneAndUpdateOptions.class, ModifyOptions.class, List.of(WriteConcern.class));
    }

    @Test
    public void findOptions() {
        checkMinDriverVersion(DriverVersion.v52);
        beanScan(FindIterable.class, FindOptions.class, List.of("explain", "filter", "projection"));
    }

    @Test
    public void insertManyOptions() {
        scan(com.mongodb.client.model.InsertManyOptions.class, InsertManyOptions.class, List.of(WriteConcern.class));
    }

    @Test
    public void insertOneOptions() {
        scan(com.mongodb.client.model.InsertOneOptions.class, InsertOneOptions.class, List.of(WriteConcern.class));
    }

    @Test
    public void replaceOptions() {
        scan(com.mongodb.client.model.ReplaceOptions.class, ReplaceOptions.class, List.of(WriteConcern.class));
    }

    @Test
    public void updateOptions() {
        scan(com.mongodb.client.model.UpdateOptions.class, UpdateOptions.class, List.of(WriteConcern.class));
    }

    private void beanScan(Class<?> driver, Class<?> morphia, List<String> filtered) {
        Method[] methods = driver.getDeclaredMethods();
        for (Method method : methods) {
            if (!filtered.contains(method.getName()) && method.getAnnotation(Deprecated.class) == null) {
                try {
                    morphia.getDeclaredMethod(method.getName(), convert(method.getParameterTypes()));
                } catch (ReflectiveOperationException e) {
                    try {
                        morphia.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException ex) {
                        throw new RuntimeException("Method not found: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void checkOverride(Class<?> driverType, Class<?> morphiaType, Method method) throws NoSuchMethodException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Method morphiaMethod = morphiaType.getMethod(method.getName(), parameterTypes);
        Assert.assertTrue(!method.getReturnType().equals(driverType)
                || morphiaMethod.getReturnType().equals(morphiaType), method.toString());

        if (parameterTypes.equals(new Class[] { Bson.class })) {
            Assert.assertTrue(!method.getReturnType().equals(driverType)
                    || morphiaType.getMethod(method.getName(), Document.class)
                            .getReturnType().equals(morphiaType),
                    method.toString());

        }
    }

    @SuppressWarnings("rawtypes")
    private Class<?>[] convert(Class[] types) {
        List<Class> list = Stream.of(types)
                .map(type -> {
                    if (type.equals(Bson.class)) {
                        return Document.class;
                    }
                    if (type.equals(BsonValue.class)) {
                        return String.class;
                    }
                    if (type.equals(Boolean.class)) {
                        return boolean.class;
                    }
                    if (type.equals(Character.class)) {
                        return char.class;
                    }
                    if (type.equals(Byte.class)) {
                        return byte.class;
                    }
                    if (type.equals(Double.class)) {
                        return double.class;
                    }
                    if (type.equals(Float.class)) {
                        return float.class;
                    }
                    if (type.equals(Integer.class)) {
                        return int.class;
                    }
                    if (type.equals(Long.class)) {
                        return long.class;
                    }
                    return type;
                })
                .toList();
        return list.toArray(new Class[0]);
    }

    private boolean getter(Method method) {
        return method.getName().startsWith("get") || method.getName().startsWith("is");
    }

    private void scan(Class<?> driverType, Class<?> morphiaType, List<Class<?>> localFields) {
        try {
            Method[] methods = driverType.getDeclaredMethods();
            Assert.assertEquals(driverType.equals(morphiaType.getSuperclass()), !Modifier.isFinal(driverType.getModifiers()),
                    "Options class should be a subclass");
            for (Method method : methods) {
                if (method.getAnnotation(Deprecated.class) == null && !method.getName().equals("builder") && !getter(method)) {
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
