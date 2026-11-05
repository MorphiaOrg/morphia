package dev.morphia.config.converters;

import java.lang.reflect.InvocationTargetException;

import com.mongodb.lang.Nullable;

import dev.morphia.EntityListener;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.MappingException;
import dev.morphia.sofia.Sofia;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * @hidden
 * @since 3.0
 * @morphia.internal
 */
@MorphiaInternal
public class EntityListenerConverter implements Converter<EntityListener<?>> {
    private final ClassLoader classLoader;

    /**
     * Loads listener classes through the thread's context class loader.
     */
    public EntityListenerConverter() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * @param classLoader the class loader to load listener classes through
     */
    public EntityListenerConverter(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    @Nullable
    public EntityListener<?> convert(String value) {
        String name = value.trim();
        if (name.isEmpty()) {
            return null;
        }
        Class<?> type;
        try {
            type = Class.forName(name, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new MappingException(Sofia.listenerClassNotFound(name), e);
        } catch (LinkageError e) {
            // loading initializes the class, so a failing static initializer surfaces here
            Throwable failure = e instanceof ExceptionInInitializerError initializer && initializer.getCause() != null
                    ? initializer.getCause()
                    : e;
            throw new MappingException(Sofia.listenerNotCreated(name, failure), e);
        }
        if (!EntityListener.class.isAssignableFrom(type)) {
            throw new MappingException(Sofia.notAnEntityListener(name));
        }
        try {
            return (EntityListener<?>) type.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new MappingException(Sofia.noargConstructorNotFound(type), e);
        } catch (ReflectiveOperationException e) {
            // a constructor's own exception arrives wrapped in an InvocationTargetException, which has no message
            Throwable failure = e instanceof InvocationTargetException invocation && invocation.getCause() != null
                    ? invocation.getCause()
                    : e;
            throw new MappingException(Sofia.listenerNotCreated(name, failure), failure);
        }
    }
}
