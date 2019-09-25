package dev.morphia.mapping.codec.references;

import dev.morphia.mapping.experimental.MorphiaReference;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ReferenceProxy implements MorphiaProxy, InvocationHandler {
    private static final List<String> NONFETCHES = List.of(
        "isEmpty", "size");
    private MorphiaReference<Object> reference;

    public ReferenceProxy(final MorphiaReference<Object> reference) {
        this.reference = reference;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (method.getName().equals("isFetched")) {
            return isFetched();
//        } else if (method.getName().equals("getClass")) {
//            return reference.getType();
        }

            fetch(method);
        return invoke(method, args);
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
        if(!isFetched() && !NONFETCHES.contains(method.getName())) {
            reference.get();
        }
    }

    private Object invoke(final Method method, final Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(isFetched() ? reference.get() : reference.getIds(), args);
    }
}
