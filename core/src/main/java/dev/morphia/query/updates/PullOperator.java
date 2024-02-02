package dev.morphia.query.updates;

import dev.morphia.Datastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.filters.Filter;

import org.bson.Document;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

/**
 * Defines an operator for $pull
 *
 * @since 2.0
 * @morphia.internal
 */
@MorphiaInternal
public class PullOperator extends UpdateOperator {
    /**
     * @param field the field
     * @param value a value object or filter to apply
     * @morphia.internal
     */
    @MorphiaInternal
    public PullOperator(String field, Object value) {
        super("$pull", field, value);
    }

    @Override
    public OperationTarget toTarget(PathTarget pathTarget) {
        Object value = value();
        if (value instanceof Filter) {
            return new OperationTarget(pathTarget, value) {
                @Override
                public Object encode(Datastore datastore) {
                    DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());
                    document(writer, () -> {
                        ((Filter) getValue()).encode(datastore, writer, EncoderContext.builder().build());
                    });
                    return new Document(pathTarget.translatedPath(), writer.getDocument());
                }
            };
        }

        return new OperationTarget(pathTarget, value);
    }
}
