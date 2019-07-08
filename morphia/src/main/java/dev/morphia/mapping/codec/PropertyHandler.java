package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class PropertyHandler {
    private final Field field;
    private final String propertyName;
    private MappedClass mappedClass;
    private Datastore datastore;
    private final TypeData typeData;

    public PropertyHandler(final Datastore datastore, final Field field, final String propertyName, final TypeData typeData) {
        this.datastore = datastore;
        this.propertyName = propertyName;
        this.field = field;
        this.typeData = typeData;
    }

    public Datastore getDatastore() {
        return datastore;
    }

    public Field getField() {
        return field;
    }

    protected MappedClass getFieldMappedClass() {
        if(mappedClass == null) {
            Class<?> type = typeData.getTypeParameters().size() == 0
                ? typeData.getType()
                : ((TypeData) typeData.getTypeParameters().get(typeData.getTypeParameters().size() - 1)).getType();
            if(type.isArray()) {
                type = type.getComponentType();
            }
            mappedClass = datastore.getMapper().getMappedClass(type);
        }
        return mappedClass;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public TypeData getTypeData() {
        return typeData;
    }

    public abstract <S, E> void set(final E instance, final PropertyModel<S> propertyModel, final S value, final Datastore datastore,
                                    final Map<Object, Object> entityCache);

    public abstract <T, S> S decodeProperty(final BsonReader reader,
                                            final DecoderContext decoderContext,
                                            final InstanceCreator<T> instanceCreator,
                                            final String name,
                                            final PropertyModel<S> propertyModel);

    public abstract <S, T> void encodeProperty(final BsonWriter writer,
                                               final T instance,
                                               final EncoderContext encoderContext,
                                               final PropertyModel<S> propertyModel);

    public abstract <S> Object encodeValue(S value);
}
