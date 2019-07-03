package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.PropertyModel;

import java.util.Map;
import java.util.function.BiConsumer;

public interface MorphiaInstanceCreator<T> extends InstanceCreator<T> {
    boolean hasHandler(PropertyModel propertyModel);

    void defer(final BiConsumer<Datastore, Map<Object, Object>> function);

    <S> PropertyHandler getHandler(final PropertyModel<S> propertyModel);
}
