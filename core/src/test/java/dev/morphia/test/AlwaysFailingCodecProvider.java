package dev.morphia.test;

import dev.morphia.query.QueryException;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

class AlwaysFailingCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> aClass, CodecRegistry codecRegistry) {
        return new Codec<T>() {
            @Override
            public T decode(BsonReader bsonReader, DecoderContext decoderContext) {
                throw new QueryException("custom codec used on decode");
            }

            @Override
            public void encode(BsonWriter bsonWriter, T t, EncoderContext encoderContext) {
                throw new QueryException("custom codec used on encode");
            }

            @Override
            public Class<T> getEncoderClass() {
                return (Class<T>) Object.class;
            }
        };
    }
}
