package dev.morphia.query.updates;

import java.util.LinkedHashMap;
import java.util.Map;

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
 * @since 2.0
 */
@MorphiaInternal
class SetOnInsertOperator extends UpdateOperator {
    private final Map<String, Object> insertValues;

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
