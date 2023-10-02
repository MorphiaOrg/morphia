package dev.morphia.query.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.filters.Filter;

import org.bson.Document;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;

/**
 * Defines an operator for $pull
 *
 * @since 2.0
 * @morphia.internal
 */
@MorphiaInternal
public class PullOperator extends UpdateOperator {
    /**
     * @param field  the field
     * @param filter the filter to apply
     * @morphia.internal
     */
    @MorphiaInternal
    public PullOperator(String field, Filter filter) {
        super("$pull", field, filter);
    }

    @Override
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(pathTarget, value()) {
            @Override
            public Object encode(MorphiaDatastore datastore) {
                DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());
                document(writer, () -> {
                    ((Filter) getValue()).encode(datastore, writer, EncoderContext.builder().build());
                });

                return new Document(pathTarget.translatedPath(), writer.getDocument());
            }
        };
    }
}
