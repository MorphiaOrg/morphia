package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Method;

public class MethodAccessor implements PropertyAccessor<Object> {
    private final Method getter;
    private final Method setter;

    public MethodAccessor(Method getter, Method setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public <S> Object get(S instance) {
        try {
            return getter.invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public <S> void set(S instance, Object value) {
        try {
            setter.invoke(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
