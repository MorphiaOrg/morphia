package dev.morphia.mapping.lifecycle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dev.morphia.annotations.internal.MorphiaInternal;

/**
 * Adapts a global listener named in {@code MorphiaConfig.listeners()}. It's found and called like an
 * {@code @EntityListeners} class, but an exception its method throws reaches the caller as it is, as it did when such
 * listeners were added with {@code Mapper.addInterceptor}: a validation listener's {@code ValidationException}, say.
 *
 * @hidden
 * @morphia.internal
 * @since 3.0
 */
@MorphiaInternal
public class GlobalListenerAdapter extends EntityListenerAdapter {
    /**
     * @param listenerType the listener class
     */
    public GlobalListenerAdapter(Class<?> listenerType) {
        super(listenerType);
    }

    @Override
    protected RuntimeException failure(Method method, Object entity, Exception e) {
        if (e instanceof InvocationTargetException invocation) {
            Throwable cause = invocation.getCause();
            if (cause instanceof RuntimeException runtime) {
                return runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
        }
        return super.failure(method, entity, e);
    }
}
