package dev.morphia.query;

import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.util.StringJoiner;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.codecs.ExpressionHelper.value;

/**
 * @morphia.internal
 */
public class OperationTarget {
    private final PathTarget target;
    private final Object value;

    /**
     * @param target the target
     * @param value  the value
     * @morphia.internal
     */
    public OperationTarget(@Nullable PathTarget target, @Nullable Object value) {
        this.target = target;
        this.value = value;
    }

    /**
     * Encodes this target
     *
     * @param datastore the datastore
     * @return the encoded form
     * @morphia.internal
     */
    public Object encode(Datastore datastore) {
        if (target == null) {
            if (value == null) {
                throw new NullPointerException();
            }
            return value;
        }
        PropertyModel mappedField = this.target.getTarget();
        Object mappedValue = value;

        PropertyModel model = mappedField != null
                              ? mappedField.getEntityModel()
                                           .getProperty(mappedField.getName())
                              : null;

        Codec cachedCodec = null;
        if (model != null && !(mappedValue instanceof LegacyQuery)) {
            cachedCodec = model.specializeCodec(datastore);
        }
        if (cachedCodec instanceof PropertyHandler) {
            mappedValue = ((PropertyHandler) cachedCodec).encode(mappedValue);
        } else {
            DocumentWriter writer = new DocumentWriter(datastore.getMapper());
            Object finalMappedValue = mappedValue;
            document(writer, () -> value(datastore, writer, "mapped", finalMappedValue, EncoderContext.builder().build()));
            mappedValue = writer.getDocument().get("mapped");
        }
        return new Document(target.translatedPath(), mappedValue);
    }

    /**
     * @return the PathTarget for this instance
     */
    @Nullable
    public PathTarget getTarget() {
        return target;
    }

    /**
     * @return the value
     */
    @Nullable
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OperationTarget.class.getSimpleName() + "[", "]")
            .add("target=" + target)
            .add("value=" + value)
            .toString();
    }
}
