package dev.morphia.query;

import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

/**
 * @hidden
 * @morphia.internal
 */
@Deprecated(forRemoval = true)
@MorphiaInternal
public class OperationTarget {
    private final PathTarget target;
    private final Object value;

    /**
     * @param target the target
     * @param value  the value
     */
    @MorphiaInternal
    public OperationTarget(@Nullable PathTarget target, @Nullable Object value) {
        this.target = target;
        this.value = value;
    }

    /**
     * Encodes this target
     *
     * @param datastore the datastore
     * @return the encoded form
     */
    @MorphiaInternal
    public Object encode(MorphiaDatastore datastore) {
        if (target == null) {
            if (value == null) {
                throw new NullPointerException();
            }
            return value;
        }
        PropertyModel mappedField = this.target.target();

        PropertyModel model = mappedField != null
                ? mappedField.getEntityModel()
                        .getProperty(mappedField.getName())
                : null;

        Codec cachedCodec = null;
        Object mappedValue;
        if (model != null) {
            cachedCodec = model.specializeCodec(datastore);
        }
        if (cachedCodec instanceof PropertyHandler) {
            mappedValue = ((PropertyHandler) cachedCodec).encode(value);
        } else {
            DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());
            document(writer, () -> value(datastore.getCodecRegistry(), writer, "mapped", value, EncoderContext.builder().build()));
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
