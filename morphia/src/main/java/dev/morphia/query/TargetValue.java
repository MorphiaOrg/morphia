package dev.morphia.query;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.PropertyCodec;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
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

        PropertyModel<?> propertyModel = mappedField != null
                                         ? mappedField.getDeclaringClass()
                                                      .getMorphiaModel()
                                                      .getPropertyModel(mappedField.getJavaFieldName())
                                         : null;

        Codec cachedCodec = propertyModel.getCachedCodec();
        if(cachedCodec instanceof PropertyHandler) {
            mappedValue = ((PropertyHandler)cachedCodec).prepare(mappedValue);
        }

//        if (cachedCodec.getEncoderClass().isAssignableFrom(componentType)) {
            DocumentWriter writer = new DocumentWriter();
            cachedCodec.encode(writer, mappedValue, EncoderContext.builder().build());
            mappedValue = writer.getRoot();
//        }
        return new Document(target.translatedPath(), mappedValue);
    }
}
