package dev.morphia.mapping.codec.references;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.IdGetter;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.sofia.Sofia;

/**
 * The proxy for lazy references.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class ReferenceProxy implements MorphiaProxy, InvocationHandler {
    private final Supplier<Object> loader;
    private final List<Object> ids;
    private final Class<?> type;
    private Object value;
    private boolean fetched;

    ReferenceProxy(Supplier<Object> loader, List<Object> ids, Class<?> type) {
        this.loader = loader;
        this.ids = ids;
        this.type = type;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if ("isFetched".equals(method.getName())) {
            return isFetched();
        } else if (method.getAnnotation(IdGetter.class) != null) {
            return ids.isEmpty() ? null : ids.get(0);
        } else if ("isEmpty".equals(method.getName())) {
            return fetched ? invokeOnValue(method, args) : ids.isEmpty();
        } else if ("size".equals(method.getName())) {
            return fetched ? invokeOnValue(method, args) : ids.size();
        } else {
            load();
            return invokeOnValue(method, args);
        }
    }

    @Override
    public boolean isFetched() {
        return fetched;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap() {
        return (T) load();
    }

    private Object load() {
        if (!fetched) {
            value = loader.get();
            fetched = true;
        }
        return value;
    }

    @Nullable
    private Object invokeOnValue(Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (method.getDeclaringClass().isAssignableFrom(getClass())) {
            return method.invoke(this, args);
        }
        if (value == null) {
            throw new ReferenceException(Sofia.missingReferencedEntity(type));
        }
        return method.invoke(value, args);
    }
}
