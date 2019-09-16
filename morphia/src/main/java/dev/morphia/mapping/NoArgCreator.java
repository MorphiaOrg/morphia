package dev.morphia.mapping;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import dev.morphia.mapping.codec.PropertyCodec;
import dev.morphia.sofia.Sofia;
import org.bson.codecs.pojo.PropertyModel;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @morphia.internal
 * @param <E>
 */
public class NoArgCreator<E> implements MorphiaInstanceCreator<E> {
    private E instance;
    private Datastore datastore;
    private Constructor<E> noArgsConstructor;
    private Map<String, PropertyCodec> handlerMap;
    private List<BiConsumer<Datastore, Map<Object, Object>>> handlers = new ArrayList<>();

    public NoArgCreator(final Datastore datastore, final Constructor<E> noArgsConstructor, Map<String, PropertyCodec> handlerMap) {
        this.datastore = datastore;
        this.noArgsConstructor = noArgsConstructor;
        this.handlerMap = handlerMap;
    }

    private E instance() {
        if (instance == null) {
            try {
                instance = noArgsConstructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new MappingException(Sofia.instantiationProblem(noArgsConstructor.getDeclaringClass().getName(), e.getMessage()), e);
            }
        }
        return instance;
    }

    @Override
    public <S> void set(final S value, final PropertyModel<S> propertyModel) {
//        final PropertyHandler propertyHandler = getHandler(propertyModel);
//        if(propertyHandler != null) {
//            defer((datastore, entityCache) -> propertyHandler.set(instance(), propertyModel, value, datastore, entityCache));
//        } else {
            propertyModel.getPropertyAccessor().set(instance(), value);
//        }
    }

    @Override
    public <S> PropertyCodec getHandler(final PropertyModel<S> propertyModel) {
        return handlerMap.get(propertyModel.getName());
    }

    @Override
    public boolean hasHandler(final PropertyModel propertyModel) {
        return handlerMap.get(propertyModel.getName()) != null;
    }

    @Override
    public E getInstance() {
        E instance = instance();
        Map<Object, Object> cache = new HashMap<>();
        for (BiConsumer<Datastore, Map<Object, Object>> deferral : handlers) {
            deferral.accept(datastore, cache);
        }
        return instance;
    }

    @Override
    public void defer(final BiConsumer<Datastore, Map<Object, Object>> function) {
        handlers.add(function);
    }

}
