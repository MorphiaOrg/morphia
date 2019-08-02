package dev.morphia.query;

import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

public class FieldCriteriaCodec implements Codec<FieldCriteria> {
    private Mapper mapper;

    public FieldCriteriaCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public FieldCriteria decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.persistenceNotIntended());
    }

    @Override
    public void encode(final BsonWriter writer, final FieldCriteria criteria, final EncoderContext encoderContext) {
        boolean not = criteria.isNot();
        FilterOperator operator = criteria.getOperator();
        String field = criteria.getField();
        Object value = criteria.getValue();
        final CodecRegistry registry = mapper.getCodecRegistry();

        if (FilterOperator.EQUAL.equals(operator)) {
            // no operator, prop equals (or NOT equals) value
            writer.writeName(field);
            if (not) {
                writer.writeStartDocument("$not");
            }
            Codec codec = registry.get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
            if (not) {
                writer.writeEndDocument();
            }

        } else {
            final Object object = null; // = obj.get(field); // operator within inner object
            if (!(object instanceof Map)) {
                writer.writeStartDocument(field);
            } else {
                if (1 == 1) {
                    //TODO:  implement this
                    throw new UnsupportedOperationException();
                }
//                inner = (Map<String, Object>) object;
            }

            if (not) {
                writer.writeStartDocument("$not");
            }

            writer.writeStartDocument(operator.val());
            Codec codec = registry.get(value.getClass());
            codec.encode(writer, value, encoderContext);
            if (not) {
                writer.writeEndDocument();
            }
            writer.writeEndDocument();

        }
    }

    @Override
    public Class<FieldCriteria> getEncoderClass() {
        return FieldCriteria.class;
    }
}
