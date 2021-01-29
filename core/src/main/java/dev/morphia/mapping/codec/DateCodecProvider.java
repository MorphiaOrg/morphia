package dev.morphia.mapping.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.sql.Timestamp;

public class DateCodecProvider implements CodecProvider {

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (java.sql.Date.class.equals(clazz)) {
            return (Codec<T>) new SqlDateCodec();
        } else if (java.sql.Time.class.equals(clazz)) {
            return (Codec<T>) new SqlTimeCodec();
        } else if (java.sql.Timestamp.class.equals(clazz)) {
            return (Codec<T>) new SqlTimestampCodec();
        } else {
            return null;
        }
    }

}
