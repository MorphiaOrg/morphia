package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * Defines an operator for $pull
 *
 * @since 2.0
 */
public class PullOperator extends UpdateOperator {
    /**
     * @param field  the field
     * @param filter the filter to apply
     * @morphia.internal
     */
    public PullOperator(String field, Filter filter) {
        super("$pull", field, filter);
    }

    @Override
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(pathTarget, value()) {
            @Override
            public Object encode(Mapper mapper) {
                DocumentWriter writer = new DocumentWriter();
                document(writer, () -> {
                    ((Filter) getValue())
                        .encode(mapper, writer, EncoderContext.builder().build());
                });

                return new Document(field(), writer.getDocument());
            }
        };
    }
}
