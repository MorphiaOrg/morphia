package dev.morphia.mapping.lifecycle;

import java.lang.reflect.Method;

import com.mongodb.lang.Nullable;

import static java.lang.String.format;

public class OnEntityListenerAdapter extends EntityListenerAdapter {
    private OnEntityListenerAdapter(Class<?> listenerType) {
        super(listenerType);
    }

    @Nullable
    public static OnEntityListenerAdapter listen(Class<?> type) {
        OnEntityListenerAdapter adapter = new OnEntityListenerAdapter(type);
        return adapter.hasCallbacks() ? adapter : null;
    }

    /**
     * The lifecycle methods are the entity's own.
     */
    @Override
    protected Object target(Object entity) {
        return entity;
    }

    @Override
    protected RuntimeException failure(Method method, Object entity, Exception e) {
        if (e instanceof IllegalArgumentException && "object is not an instance of declaring class".equals(e.getMessage())) {
            return new RuntimeException(format("%s is not an instance of the method's type: %s",
                    entity.getClass().getName(), method.getDeclaringClass().getName()), e);
        }
        return new RuntimeException("Failed to invoke method: " + method, e);
    }
}
