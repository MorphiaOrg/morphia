package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.PropertyCodec;
import org.bson.codecs.pojo.InstanceCreatorFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @morphia.internal
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class InstanceCreatorFactoryImpl<T> implements InstanceCreatorFactory<T> {
    private Constructor<T> noArgsConstructor;
    private Datastore datastore;
    private Map<String, PropertyCodec> handlers = new HashMap<>();

    public InstanceCreatorFactoryImpl(final Datastore datastore, final Class type) {
        this.datastore = datastore;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                noArgsConstructor = (Constructor<T>) constructor;
                noArgsConstructor.setAccessible(true);
            }
        }
        if(!type.isInterface() && noArgsConstructor == null) {
            throw new MappingException("Can not find a no arg constructor for " + type);
        }
    }

    @Override
    public MorphiaInstanceCreator<T> create() {
        return new NoArgCreator<>(datastore, noArgsConstructor, handlers);
    }

    void register(final PropertyCodec handler) {
        handlers.put(handler.getPropertyName(), handler);
    }

}
