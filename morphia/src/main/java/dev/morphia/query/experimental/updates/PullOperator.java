package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.query.OperationTarget;
import dev.morphia.query.experimental.filters.Filter;
import org.bson.codecs.EncoderContext;

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
    public PullOperator(final String field, final Filter filter) {
        super("$pull", field, filter);
    }

    @Override
    public OperationTarget toTarget(final PathTarget pathTarget) {
        return new OperationTarget(pathTarget, value()) {
            @Override
            public Object encode(final Mapper mapper) {
                DocumentWriter writer = new DocumentWriter();
                writer.writeStartDocument();
                ((Filter) getValue()).encode(mapper, writer, EncoderContext.builder().build());
                writer.writeEndDocument();

                return writer.getDocument();
            }
        };
    }
}
