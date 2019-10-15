package dev.morphia.query;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.PropertyModel;

import java.util.StringJoiner;

import static dev.morphia.mapping.codec.MorphiaCodecProvider.isMappable;

class OperationTarget {
    private PathTarget target;
    private Object value;

    public OperationTarget(final PathTarget target, final Object value) {
        this.target = target;
        this.value = value;
    }

    public PathTarget getTarget() {
        return target;
    }

    public Object getValue() {
        return value;
    }

    public Object encode(final Mapper mapper) {
        if(target == null) {
            return value;
        }
        MappedField mappedField = this.target.getTarget();
        Object mappedValue = value;

        PropertyModel<?> propertyModel = mappedField != null
                                         ? mappedField.getDeclaringClass()
                                                      .getMorphiaModel()
                                                      .getPropertyModel(mappedField.getJavaFieldName())
                                         : null;

        Codec cachedCodec = propertyModel != null ? propertyModel.getCachedCodec() : null;
        if (cachedCodec instanceof PropertyHandler) {
            mappedValue = ((PropertyHandler) cachedCodec).encode(mappedValue);
        } else if (mappedValue != null) {
            Codec codec = mapper.getCodecRegistry().get(mappedValue.getClass());
            DocumentWriter writer = new DocumentWriter();
            codec.encode(writer, mappedValue, EncoderContext.builder().build());
            mappedValue = writer.getRoot();
        }
        return new Document(target.translatedPath(), mappedValue);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OperationTarget.class.getSimpleName() + "[", "]")
                   .add("target=" + target)
                   .add("value=" + value)
                   .toString();
    }
}
