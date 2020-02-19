package dev.morphia.query;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.StringJoiner;

/**
 * @morphia.internal
 */
public class OperationTarget {
    private PathTarget target;
    private Object value;

    public OperationTarget(final PathTarget target, final Object value) {
        this.target = target;
        this.value = value;
    }

    /**
     * @return the PathTarget for this instance
     */
    public PathTarget getTarget() {
        return target;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    public Object encode(final Mapper mapper) {
        if (target == null) {
            return value;
        }
        MappedField mappedField = this.target.getTarget();
        Object mappedValue = value;

        FieldModel<?> model = mappedField != null
                              ? mappedField.getDeclaringClass()
                                           .getEntityModel()
                                           .getFieldModelByName(mappedField.getJavaFieldName())
                              : null;

        Codec cachedCodec = model != null ? model.getCachedCodec() : null;
        if (cachedCodec instanceof PropertyHandler) {
            mappedValue = ((PropertyHandler) cachedCodec).encode(mappedValue);
        } else if (mappedValue != null) {
            Codec codec = mapper.getCodecRegistry().get(mappedValue.getClass());
            DocumentWriter writer = new DocumentWriter();
            writer.writeStartDocument();
            writer.writeName("mapped");
            codec.encode(writer, mappedValue, EncoderContext.builder().build());
            writer.writeEndDocument();
            mappedValue = writer.getDocument().get("mapped");
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
