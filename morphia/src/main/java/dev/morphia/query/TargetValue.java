package dev.morphia.query;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PropertyModel;

import java.util.Iterator;

class TargetValue {
    private PathTarget target;
    private Object value;

    public TargetValue(final PathTarget target, final Object value) {
        this.target = target;
        this.value = value;
    }

    public Object encode() {
        MappedField mappedField = this.target.getTarget();
        Object mappedValue = value;
//        final Class<?> type = (mappedValue == null) ? null : mappedValue.getClass();

        PropertyModel<?> propertyModel = mappedField != null
                                         ? mappedField.getDeclaringClass()
                                                      .getMorphiaModel()
                                                      .getPropertyModel(mappedField.getJavaFieldName())
                                         : null;

/*
        Class<?> componentType = type;
        if (componentType.isArray() || Iterable.class.isAssignableFrom(componentType)) {
            if (type.isArray()) {
                componentType = type.getComponentType();
            } else {
                Iterator iterator = ((Iterable) value).iterator();
                if (iterator.hasNext()) {
                    componentType = iterator.next().getClass();
                }
            }
        }
*/
        Codec cachedCodec = propertyModel.getCachedCodec();
//        if (cachedCodec.getEncoderClass().isAssignableFrom(componentType)) {
            DocumentWriter writer = new DocumentWriter();
            cachedCodec.encode(writer, value, EncoderContext.builder().build());
            mappedValue = writer.getRoot();
//        }
        return new Document(target.translatedPath(), mappedValue);
    }
}
