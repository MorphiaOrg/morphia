package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class Accumulator extends Expression {
    public Accumulator(final String operation) {
        super(operation);
    }

    public Accumulator(final String operation, final String name) {
        super(operation, name);
    }

    protected Accumulator(final String operation, final String name, final Object value) {
        super(operation, name, value);
    }

    public static Accumulator sum(final String name, final Expression value, final Expression... additional) {
        return new SumAccumulator(name, value, additional);
    }

    private static class SumAccumulator extends Accumulator {
        private List<Expression> expressions = new ArrayList<>();
        public SumAccumulator(final String name,
                              final Expression value,
                              final Expression[] additional) {
            super("$sum", name);
            expressions.add(value);
            expressions.addAll(asList(additional));
        }

        @Override
        public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
            writer.writeStartDocument(name);
            writer.writeName(operation);
            if(expressions.size() >1 ) {
                writer.writeStartArray();
            }
            for (final Expression expression : expressions) {
                writeUnnamedExpression(mapper, writer, expression, encoderContext);
            }
            if(expressions.size() >1 ) {
                writer.writeEndArray();
            }
            writer.writeEndDocument();
        }
    }
}
