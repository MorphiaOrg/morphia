package dev.morphia.mapping.codec.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateException;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.unset;

/**
 * @param <T> the entity type
 * @morphia.internal
 * @since 2.2
 */
@MorphiaInternal
public class MergingEncoder<T> extends EntityEncoder {
    private final Query<T> query;
    private final DocumentWriter setOperations;
    private List<UpdateOperator> update;

    /**
     * @param query        the query
     * @param morphiaCodec the codec
     * @morphia.internal
     * @since 2.2
     */
    @MorphiaInternal
    public MergingEncoder(Query<T> query, MorphiaCodec<T> morphiaCodec) {
        super(morphiaCodec);
        this.query = query;
        setOperations = new DocumentWriter(morphiaCodec.getMapper().getConfig());
    }

    /**
     * @param entity
     * @return the update
     */
    public List<UpdateOperator> encode(Object entity) {
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
            update = new ArrayList<>();
        }
        update.add(operator);
    }
}
