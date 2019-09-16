package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.InstanceCreator;
import org.bson.codecs.pojo.PropertyModel;
import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class PropertyCodec<T> implements Codec<T> {
    private final Field field;
    private final String propertyName;
    private MappedClass mappedClass;
    private Datastore datastore;
    private final TypeData typeData;

    public PropertyCodec(final Datastore datastore, final Field field, final String propertyName, final TypeData typeData) {
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

}
