package dev.morphia.query.updates;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.EntityAware;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;

import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;

/**
 * @morphia.internal
 * @hidden
 * @since 2.0
 */
@MorphiaInternal
public class SetOnInsertOperator extends UpdateOperator implements EntityAware {
    private final Map<String, Object> insertValues;

    @Nullable
    private EntityModel model;

    /**
     * @param values the values
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public SetOnInsertOperator(Map<String, Object> values) {
        super("$setOnInsert", "unused", "unused");
        insertValues = new LinkedHashMap<>(values);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @Nullable
    @MorphiaInternal
    public EntityModel model() {
        return model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends EntityAware> T entityModel(@Nullable EntityModel model) {
        this.model = model;
        return (T) this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Map<String, Object> insertValues() {
        return insertValues;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);

        Mapper mapper = pathTarget.mapper();
        DocumentWriter writer = new DocumentWriter(mapper.getConfig());
        document(writer, () -> {
            insertValues.forEach((key, value) -> {
                PathTarget keyTarget = new PathTarget(mapper, model, key, true);
                writer.writeName(keyTarget.translatedPath());
                Codec valueCodec = datastore.getCodecRegistry().get(value.getClass());
                valueCodec.encode(writer, value, EncoderContext.builder().build());
            });
        });

        return new OperationTarget(null, null) {
            @Override
            public Object encode(MorphiaDatastore datastore) {
                return writer.getDocument();
            }
        };
    }
}
