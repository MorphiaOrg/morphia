package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class Accumulator extends Expression {
    private List<Expression> expressions = new ArrayList<>();

    protected Accumulator(final String operation, final List<Expression> values) {
        super(operation);
        expressions.addAll(values);
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
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

    public static Accumulator sum(final Expression value, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(value);
        expressions.addAll(asList(additional));
        return new Accumulator("$sum", expressions);
    }

    public static Accumulator add(final Expression value, final Expression... additional) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(value);
        expressions.addAll(asList(additional));
        return new Accumulator("$add", expressions);
    }
}
