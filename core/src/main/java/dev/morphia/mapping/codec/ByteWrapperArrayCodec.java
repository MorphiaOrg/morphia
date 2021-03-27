package dev.morphia.mapping.codec;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Encodes a Byte[] to BinData like the driver's ByteArrayCodec does for byte[]
 *
 * @since 2.1.5
 */
public class ByteWrapperArrayCodec implements Codec<Byte[]> {
    @Override
    public Byte[] decode(BsonReader reader, DecoderContext decoderContext) {
        return wrapper(reader.readBinaryData().getData());
    }

    @Override
    public void encode(BsonWriter writer, Byte[] value, EncoderContext encoderContext) {
        writer.writeBinaryData(new BsonBinary(primitive(value)));
    }

    @Override
    public Class<Byte[]> getEncoderClass() {
        return Byte[].class;
    }

    private byte[] primitive(Byte[] value) {
        byte[] array = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            array[i] = value[i];
        }

        return array;
    }

    private Byte[] wrapper(byte[] value) {
        Byte[] array = new Byte[value.length];
        for (int i = 0; i < value.length; i++) {
            array[i] = value[i];
        }

        return array;
    }

}
