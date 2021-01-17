package dev.morphia.query;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.StringJoiner;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

/**
 * @morphia.internal
 */
public class OperationTarget {
    private final PathTarget target;
    private final Object value;

    /**
     * @param target the target
     * @param value the value
     * @morphia.internal
     */
    public OperationTarget(PathTarget target, Object value) {
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

    /**
     * Encodes this target
     * @param mapper the mapper
     * @return the encoded form
     * @morphia.internal
     */
    public Object encode(Mapper mapper) {
        if (target == null) {
            return value;
        }
        PropertyModel mappedField = this.target.getTarget();
        Object mappedValue = value;

        PropertyModel model = mappedField != null
                              ? mappedField.getEntityModel()
                                           .getProperty(mappedField.getName())
                              : null;

        Codec cachedCodec = model != null && !(mappedValue instanceof LegacyQuery)
                            ? model.getCachedCodec()
                            : null;
        if (cachedCodec instanceof PropertyHandler) {
            mappedValue = ((PropertyHandler) cachedCodec).encode(mappedValue);
        } else if (mappedValue != null) {
            DocumentWriter writer = new DocumentWriter();
            Object finalMappedValue = mappedValue;
            document(writer, () -> value(mapper, writer, "mapped", finalMappedValue, EncoderContext.builder().build()));
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
