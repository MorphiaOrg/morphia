package dev.morphia.mapping.codec;

import org.bson.BSONException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * Defines a codec for URIs
 */
public class URICodec implements Codec<URI> {
    @Override
    public URI decode(BsonReader reader, DecoderContext decoderContext) {
        return URI.create(reader.readString());
    }

    @Override
    public void encode(BsonWriter writer, URI value, EncoderContext encoderContext) {
        try {
            writer.writeString(value.toURL().toExternalForm());
        } catch (MalformedURLException e) {
            throw new BSONException("Could not serialize the URI: " + value);
        }
    }

    @Override
    public Class<URI> getEncoderClass() {
        return URI.class;
    }
}
