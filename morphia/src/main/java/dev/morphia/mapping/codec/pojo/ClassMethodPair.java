package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.mapping.InstanceCreatorFactoryImpl;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.lang.annotation.Annotation;
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
    private Class<? extends Annotation> event;

    ClassMethodPair(final Datastore datastore, final Method method, final Class<?> type, final Class<? extends Annotation> event) {
        this.event = event;
        this.type = type;
        this.method = method;
        this.datastore = datastore;
    }

    void invoke(final Document document, final Object entity) {
        try {
            Object instance;
            if (type != null) {
                instance = getOrCreateInstance(type, datastore);
            } else {
                instance = entity;
            }

            final Method method = getMethod();
            method.setAccessible(true);

            Sofia.logCallingLifecycleMethod(event.getSimpleName(), method, instance);
            List<Object> args = new ArrayList<>();

            for (final Class<?> parameterType : method.getParameterTypes()) {
                if (parameterType.equals(Document.class)) {
                    args.add(document);
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

    private Object getOrCreateInstance(final Class<?> clazz, final Datastore datastore) {
        final InstanceCreatorFactoryImpl creatorFactory = new InstanceCreatorFactoryImpl(clazz);
        return creatorFactory.create().getInstance();

    }

    Method getMethod() {
        return method;
    }

}
