package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappingException;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @morphia.internal
 */
public class ClassMethodPair {
    private final Class<?> type;
    private final Method method;
    private final Datastore datastore;
    private final Class<? extends Annotation> event;

    ClassMethodPair(Datastore datastore, Method method, Class<?> type, Class<? extends Annotation> event) {
        this.event = event;
        this.type = type;
        this.method = method;
        this.datastore = datastore;
    }

    void invoke(Document document, Object entity) {
        try {
            Object instance;
            if (type != null) {
                instance = getOrCreateInstance(type);
            } else {
                instance = entity;
            }

            final Method method = getMethod();
            method.setAccessible(true);

            Sofia.logCallingLifecycleMethod(event.getSimpleName(), method, instance);
            List<Object> args = new ArrayList<>();

            for (Class<?> parameterType : method.getParameterTypes()) {
                if (parameterType.equals(Document.class)) {
                    args.add(document);
                } else if (parameterType.equals(Datastore.class)) {
                    args.add(datastore);
                } else {
                    args.add(entity);
                }
            }
            if (instance == null) {
                method.invoke(args.toArray());
            } else {
                method.invoke(instance, args.toArray());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Object getOrCreateInstance(Class<?> type) {
        try {
            Constructor<?> declaredConstructor = type.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new MappingException(Sofia.cannotInstantiate(type, e.getMessage()));
        }

    }

    Method getMethod() {
        return method;
    }

}
