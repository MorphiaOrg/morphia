package dev.morphia.aggregation.experimental.expressions;

import dev.morphia.mapping.Mapper;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

public class PushExpression extends Expression {
    private List<Field> fields = new ArrayList<>();
    private String source;

    public PushExpression() {
        super("$push");
    }

    public PushExpression source(final String source) {
        this.source = source;
        return this;
    }

    public PushExpression source(final String source, final String renamed) {
        fields.add(new Field(source, renamed));
        return this;
    }

    @Override
    public void encode(final Mapper mapper, final BsonWriter writer, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName(operation);
        if (source != null) {
            writer.writeString(source.startsWith("$") ? source : "$" + source);
        } else if (fields != null) {
            writer.writeStartDocument();
            for (final Field field : fields) {
                String source = field.getSource();
                String renamed = field.getRenamed();
                if (!renamed.startsWith("$")) {
                    renamed = "$" + renamed;
                }
                writer.writeString(source, renamed);
            }
            writer.writeEndDocument();
        }
        writer.writeEndDocument();
    }

    private static class Field {
        private String source;
        private String renamed;

        public Field(final String source, final String renamed) {
            this.source = source;
            this.renamed = renamed;
        }

        public String getSource() {
            return source;
        }

        public String getRenamed() {
            return renamed;
        }
    }
}
