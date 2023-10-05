package dev.morphia.query.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.filters.Filter;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.document;

/**
 * Defines an operator for $pull
 *
 * @since 2.0
 * @morphia.internal
 */
@MorphiaInternal
class PullOperator extends UpdateOperator {
    /**
     * @param field  the field
     * @param filter the filter to apply
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public PullOperator(String field, Filter filter) {
        super("$pull", field, filter);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);

        return new OperationTarget(pathTarget, value()) {
            @Override
            public Object encode(MorphiaDatastore datastore) {
                DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());
                CodecRegistry registry = datastore.getCodecRegistry();
                document(writer, () -> {
                    Filter filter = (Filter) getValue();
                    Codec codec = registry.get(filter.getClass());
                    codec.encode(writer, filter, EncoderContext.builder().build());
                });

                return new Document(pathTarget.translatedPath(), writer.getDocument());
            }
        };
    }
}
