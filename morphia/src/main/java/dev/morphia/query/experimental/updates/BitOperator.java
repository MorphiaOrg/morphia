package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.query.OperationTarget;

class BitOperator extends UpdateOperator {
    private final String operation;

    public BitOperator(final String operation, final String field, final int value) {
        super("$bit", field, value);
        this.operation = operation;
    }

    @Override
    public OperationTarget toTarget(final PathTarget pathTarget) {
        return new OperationTarget(pathTarget, value()) {
            @Override
            public Object encode(final Mapper mapper) {
                DocumentWriter writer = new DocumentWriter();
                writer.writeStartDocument();
                writer.writeStartDocument(pathTarget.translatedPath());
                writer.writeInt32(operation, (Integer) value());
                writer.writeEndDocument();
                writer.writeEndDocument();

                return writer.getDocument();
            }
        };
    }
}
