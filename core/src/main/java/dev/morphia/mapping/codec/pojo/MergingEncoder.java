package dev.morphia.mapping.codec.pojo;

import com.mongodb.lang.Nullable;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.Query;
import dev.morphia.query.Update;
import dev.morphia.query.UpdateException;
import dev.morphia.query.experimental.updates.UpdateOperator;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.Map.Entry;

import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static dev.morphia.query.experimental.updates.UpdateOperators.unset;

/**
 * @param <T> the entity type
 * @morphia.internal
 * @since 2.2
 */
public class MergingEncoder<T> extends EntityEncoder {
    private final Query<T> query;
    private final DocumentWriter setOperations;
    private Update<T> update;

    /**
     * @param query        the query
     * @param morphiaCodec the codec
     * @morphia.internal
     * @since 2.2
     */
    public MergingEncoder(Query<T> query, MorphiaCodec<T> morphiaCodec) {
        super(morphiaCodec);
        this.query = query;
        setOperations = new DocumentWriter(morphiaCodec.getMapper());
    }

    public Update<T> encode(Object entity) {
        encode(setOperations, entity, EncoderContext.builder().build());

        for (Entry<String, Object> entry : setOperations.getDocument().entrySet()) {
            add(set(entry.getKey(), entry.getValue()));
        }

        if (update == null) {
            throw new UpdateException("Nothing to update");
        }

        return update;
    }

    @Override
    protected void encodeDiscriminator(BsonWriter writer, EntityModel model) {
    }

    @Override
    protected void encodeIdProperty(BsonWriter writer, Object instance, EncoderContext encoderContext, @Nullable PropertyModel idModel) {
    }

    @Override
    protected void encodeValue(BsonWriter writer, EncoderContext encoderContext, PropertyModel model, @Nullable Object value) {
        super.encodeValue(writer, encoderContext, model, value);
        if (!model.shouldSerialize(value)) {
            add(unset(model.getMappedName()));
        }
    }

    private void add(UpdateOperator operator) {
        if (update == null) {
            update = query.update(operator);
        } else {
            update.add(operator);
        }
    }
}
