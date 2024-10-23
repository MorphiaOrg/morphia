package dev.morphia.mapping.codec;

import dev.morphia.query.Type;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class TypeCodec implements Codec<Type> {
    @Override
    public Type decode(BsonReader bsonReader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(BsonWriter bsonWriter, Type type, EncoderContext encoderContext) {
        var value = switch (type) {
            case BINARY_DATA -> "binData";
            case OBJECT_ID -> "objectId";
            case BOOLEAN -> "bool";
            case REGULAR_EXPRESSION -> "regex";
            case DB_POINTER -> "dbPointer";
            case INTEGER_32_BIT -> "int";
            case INTEGER_64_BIT -> "long";
            case DECIMAL_128 -> "decimal";
            case MIN_KEY -> "minKey";
            case MAX_KEY -> "maxKey";
            default -> type.name().toLowerCase();
        };
        bsonWriter.writeString(value);
    }

    @Override
    public Class<Type> getEncoderClass() {
        return Type.class;
    }
}
