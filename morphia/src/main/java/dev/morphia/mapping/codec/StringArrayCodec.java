package dev.morphia.mapping.codec;

import dev.morphia.mapping.Mapper;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.List;

class StringArrayCodec implements Codec<String[]> {

    private Codec<String> codec;
    private Mapper mapper;

    StringArrayCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Class<String[]> getEncoderClass() {
        return String[].class;
    }
  
    private Codec<String> getCodec() {
        if(codec == null) {
            codec = mapper.getCodecRegistry().get(String.class);
        }
        return codec;
    }

    @Override
    public void encode(final BsonWriter writer, final String[] value, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final String cur : value) {
            getCodec().encode(writer, cur, encoderContext);
        }
        writer.writeEndArray();
    }

    @Override
    public String[] decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<String> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(getCodec().decode(reader, decoderContext));
        }

        reader.readEndArray();

        String[] array = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
