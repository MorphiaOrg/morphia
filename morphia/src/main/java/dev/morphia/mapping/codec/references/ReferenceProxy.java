package dev.morphia.mapping.codec.references;

import dev.morphia.mapping.experimental.MorphiaReference;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ReferenceProxy implements InvocationHandler {
    private final List<String> nonfetches = List.of(
        "isEmpty", "size"/*, "toString"*/);
    private final Object ids;
    private MorphiaReference<Object> reference;
    private Object wrapped;

    public ReferenceProxy(Object ids, final MorphiaReference<Object> listReference) {
        this.ids = ids;
        this.reference = listReference;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        fetch(method);
        return invoke(method, args);
    }

    private void fetch(final Method method) {
        if(wrapped == null && !nonfetches.contains(method.getName())) {
            wrapped = reference.get();
        }
    }

    private Object invoke(final Method method, final Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(wrapped != null ? wrapped : ids, args);
    }
}
