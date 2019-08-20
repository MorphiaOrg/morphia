package dev.morphia.mapping.codec.pojo;

import dev.morphia.mapping.InstanceCreatorFactoryImpl;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @morphia.internal
 */
public class ClassMethodPair {
    private Mapper mapper;
    private Class<? extends Annotation> event;
    private final Class<?> type;
    private final Method method;

    ClassMethodPair(final Mapper mapper, Class<? extends Annotation> event, final Class<?> type, final Method method) {
        this.mapper = mapper;
        this.event = event;
        this.type = type;
        this.method = method;
    }

    public Class<?> getType() {
        return type;
    }

    public Method getMethod() {
        return method;
    }

    public void invoke(final Document document, final Object entity) {
        try {
            Object instance;
            if (type != null) {
                instance = getOrCreateInstance(type, mapper);
            } else {
                instance = entity;
            }

            final Method method = getMethod();
            method.setAccessible(true);

            Sofia.logCallingLifecycleMethod(event.getSimpleName(), method, instance);
            List<Object> args = new ArrayList<>();

            for (final Class<?> parameterType : method.getParameterTypes()) {
                if(parameterType.equals(Document.class)) {
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

    private Object getOrCreateInstance(final Class<?> clazz, final Mapper mapper) {
        final InstanceCreatorFactoryImpl creatorFactory = new InstanceCreatorFactoryImpl(mapper.getDatastore(), clazz);
        return creatorFactory.create().getInstance();

    }


}
