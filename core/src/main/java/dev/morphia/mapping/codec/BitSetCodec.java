package dev.morphia.mapping.codec;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Defines a codec for standard {@link java.util.BitSet}. BitSet is
 * encoded/decoded a an array of long integers.
 * <p>
 * For compatibility with the legacy MongoDB driver, the codec can
 * also decode a BitSet from a document if it contains a key named "words"
 * which contains the array of longs. Note that this legacy format is only used
 * when LOADING a BitSet. When persisting a BitSet, it is always stored as an
 * array of long integers.
 */
public class BitSetCodec implements Codec<BitSet> {
    private static final String LEGACY_ARRAY_KEY = "words";

    @Override
    public void encode(BsonWriter writer, BitSet value, EncoderContext encoderContext) {
        writer.writeStartArray();
        long[] longs = value.toLongArray();
        for (int i = 0; i < longs.length; ++i) {
            writer.writeInt64(longs[i]);
        }
        writer.writeEndArray();
    }

    @Override
    public BitSet decode(BsonReader reader, DecoderContext decoderContext) {
        var currentType = reader.getCurrentBsonType();
        if (currentType == BsonType.DOCUMENT) {
            return decodeLegacy(reader, decoderContext);
        } else if (currentType == BsonType.ARRAY) {
            return decodeModern(reader, decoderContext);
        } else {
            throw new IllegalStateException("cannot decode BitSet from " + currentType);
        }
    }

    private BitSet decodeLegacy(BsonReader reader, DecoderContext decoderContext) {
        List<Long> temp = new ArrayList<>();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            if (fieldName.equals(LEGACY_ARRAY_KEY)) {
                reader.readStartArray();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    temp.add(reader.readInt64());
                }
                reader.readEndArray();
            } else {
                // the legacy encoder also encoded the private members of BitSet, which we ignore here: "wordsInUse" (int) and "sizeIsStick" (bool)
                switch (reader.getCurrentBsonType()) {
                    case INT32:
                        reader.readInt32();
                        break;
                    case BOOLEAN:
                        reader.readBoolean();
                        break;
                    default:
                        throw new IllegalStateException(
                                "BitSetCodec can't decode field '" + fieldName + "' of type " + reader.getCurrentBsonType());
                }
            }
        }
        reader.readEndDocument();
        return BitSet.valueOf(toArray(temp));
    }

    private BitSet decodeModern(BsonReader reader, DecoderContext decoderContext) {
        List<Long> temp = new ArrayList<>();
        reader.readStartArray();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            temp.add(reader.readInt64());
        }
        reader.readEndArray();
        return BitSet.valueOf(toArray(temp));
    }

    /**
     * Converts a list of Longs to an array of Longs.
     */
    private long[] toArray(List<Long> list) {
        var ret = new long[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            ret[i] = list.get(i);
        }
        return ret;
    }

    @Override
    public Class<BitSet> getEncoderClass() {
        return BitSet.class;
    }

}
