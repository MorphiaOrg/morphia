package dev.morphia.mapping.codec.references;

import dev.morphia.annotations.IdGetter;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.sofia.Sofia;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ReferenceProxy implements MorphiaProxy, InvocationHandler {
    private static final List<String> NONFETCHES = List.of(
        "isEmpty", "size");
    private MorphiaReference<?> reference;

    public ReferenceProxy(final MorphiaReference<?> reference) {
        this.reference = reference;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (method.getName().equals("isFetched")) {
            return isFetched();
        } else if (method.getAnnotation(IdGetter.class) != null) {
            return reference.getIds().get(0);
        } else if ("isEmpty".equals(method.getName())) {
            return isFetched() ? invoke(method, args) : reference.getIds().isEmpty();
        } else if ("size".equals(method.getName())) {
            return isFetched() ? invoke(method, args) : reference.getIds().size();
        } else {
            fetch(method);
            return invoke(method, args);
        }
    }

    @Override
    public boolean isFetched() {
        return reference.isResolved();
    }

    @Override
    public <T> T unwrap() {
        return (T) reference.get();
    }

    private void fetch(final Method method) {
        if (!isFetched() && !NONFETCHES.contains(method.getName())) {
            reference.get();
        }
    }

    private Object invoke(final Method method, final Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (method.getDeclaringClass().isAssignableFrom(getClass())) {
            return method.invoke(this, args);
        } else {
            if (isFetched()) {
                Object target = reference.get();
                if(target == null) {
                    throw new ReferenceException(Sofia.missingReferencedEntity(reference.getType()));
                }
                return method.invoke(target, args);
            } else {
                return method.invoke(reference.getIds(), args);
            }
        }
    }
}
