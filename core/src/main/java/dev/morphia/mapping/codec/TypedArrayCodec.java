package dev.morphia.mapping.codec;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.MorphiaDatastore;

import org.bson.BsonInvalidOperationException;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.BooleanCodec;
import org.bson.codecs.CharacterCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DoubleCodec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.FloatCodec;
import org.bson.codecs.IntegerCodec;
import org.bson.codecs.LongCodec;
import org.bson.codecs.ShortCodec;

import static java.lang.String.format;

class TypedArrayCodec implements Codec {
    private static final int INITIAL_CAPACITY = 16;
    // the largest buffer a size hint may ask for up front; anything longer grows from there
    private static final int MAX_HINTED_CAPACITY = 1 << 16;

    private final Class type;
    // the component type's codec, fixed for this codec; resolved on first use. volatile because this codec is shared
    // across threads
    private volatile Codec codec;
    private MorphiaDatastore datastore;

    TypedArrayCodec(MorphiaDatastore datastore, Class type) {
        this.datastore = datastore;
        this.type = type;
    }

    /**
     * Creates the codec for arrays of {@code type}. Primitive component types get a codec that reads and writes the
     * elements without boxing them.
     */
    static TypedArrayCodec of(MorphiaDatastore datastore, Class<?> type) {
        if (type == double.class) {
            return new DoubleArrayCodec(datastore);
        } else if (type == float.class) {
            return new FloatArrayCodec(datastore);
        } else if (type == int.class) {
            return new IntArrayCodec(datastore);
        } else if (type == long.class) {
            return new LongArrayCodec(datastore);
        } else if (type == short.class) {
            return new ShortArrayCodec(datastore);
        } else if (type == boolean.class) {
            return new BooleanArrayCodec(datastore);
        } else if (type == char.class) {
            return new CharArrayCodec(datastore);
        }
        return new TypedArrayCodec(datastore, type);
    }

    @Override
    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
        writer.writeStartArray();
        Codec codec = getCodec();
        if (value instanceof Object[] values) {
            for (Object element : values) {
                if (element == null) {
                    writer.writeNull();
                } else {
                    codec.encode(writer, element, encoderContext);
                }
            }
        } else {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                codec.encode(writer, Array.get(value, i), encoderContext);
            }
        }
        writer.writeEndArray();
    }

    @Override
    public Class getEncoderClass() {
        return Array.newInstance(type, 0).getClass();
    }

    Codec getCodec() {
        Codec codec = this.codec;
        if (codec == null) {
            codec = datastore.getCodecRegistry().get(type);
            this.codec = codec;
        }
        return codec;
    }

    @Override
    public Object decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartArray();

        Codec codec = getCodec();
        List list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(codec.decode(reader, decoderContext));
        }

        reader.readEndArray();

        if (!type.isPrimitive()) {
            return list.toArray((Object[]) Array.newInstance(type, list.size()));
        }
        Object array = Array.newInstance(type, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }

        return array;
    }

    @Override
    public String toString() {
        return format("%s<%s>", getClass().getName(), type.getSimpleName());
    }

    /**
     * A primitive array codec. It reads and writes elements directly while the component codec is the driver's own,
     * and reads any element not stored as the component's native BSON type through that codec, so numeric conversions
     * and their errors are unchanged. A replaced component codec gets the boxed path above.
     */
    private abstract static class PrimitiveArrayCodec extends TypedArrayCodec {
        private final Class<?> driverCodec;
        // the length of the last array decoded. arrays of one property tend to share a length (an embedding's
        // dimension, say), so starting there usually means no growth and no final copy. a stale value from another
        // thread only costs a resize, so it isn't volatile
        private int lastLength = INITIAL_CAPACITY;

        PrimitiveArrayCodec(MorphiaDatastore datastore, Class<?> type, Class<?> driverCodec) {
            super(datastore, type);
            this.driverCodec = driverCodec;
        }

        @Override
        public final void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
            if (getCodec().getClass() != driverCodec) {
                super.encode(writer, value, encoderContext);
                return;
            }
            writer.writeStartArray();
            write(writer, value);
            writer.writeEndArray();
        }

        @Override
        public final Object decode(BsonReader reader, DecoderContext decoderContext) {
            Codec codec = getCodec();
            if (codec.getClass() != driverCodec) {
                return super.decode(reader, decoderContext);
            }
            reader.readStartArray();
            Object array = read(reader, codec, decoderContext);
            reader.readEndArray();
            return array;
        }

        int capacity() {
            return Math.min(Math.max(lastLength, INITIAL_CAPACITY), MAX_HINTED_CAPACITY);
        }

        boolean exact(int length, int size) {
            lastLength = size;
            return length == size;
        }

        abstract void write(BsonWriter writer, Object value);

        abstract Object read(BsonReader reader, Codec codec, DecoderContext decoderContext);
    }

    private static final class DoubleArrayCodec extends PrimitiveArrayCodec {
        DoubleArrayCodec(MorphiaDatastore datastore) {
            super(datastore, double.class, DoubleCodec.class);
        }

        @Override
        void write(BsonWriter writer, Object value) {
            for (double element : (double[]) value) {
                writer.writeDouble(element);
            }
        }

        @Override
        Object read(BsonReader reader, Codec codec, DecoderContext decoderContext) {
            double[] values = new double[capacity()];
            int size = 0;
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (size == values.length) {
                    values = Arrays.copyOf(values, size * 2);
                }
                values[size++] = reader.getCurrentBsonType() == BsonType.DOUBLE
                        ? reader.readDouble()
                        : (Double) codec.decode(reader, decoderContext);
            }
            return exact(values.length, size) ? values : Arrays.copyOf(values, size);
        }
    }

    private static final class FloatArrayCodec extends PrimitiveArrayCodec {
        FloatArrayCodec(MorphiaDatastore datastore) {
            super(datastore, float.class, FloatCodec.class);
        }

        @Override
        void write(BsonWriter writer, Object value) {
            for (float element : (float[]) value) {
                writer.writeDouble(element);
            }
        }

        @Override
        Object read(BsonReader reader, Codec codec, DecoderContext decoderContext) {
            float[] values = new float[capacity()];
            int size = 0;
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (size == values.length) {
                    values = Arrays.copyOf(values, size * 2);
                }
                float element;
                if (reader.getCurrentBsonType() == BsonType.DOUBLE) {
                    // the range check FloatCodec makes
                    double value = reader.readDouble();
                    if (value < -Float.MAX_VALUE || value > Float.MAX_VALUE) {
                        throw new BsonInvalidOperationException(format("%s can not be converted into a Float.", value));
                    }
                    element = (float) value;
                } else {
                    element = (Float) codec.decode(reader, decoderContext);
                }
                values[size++] = element;
            }
            return exact(values.length, size) ? values : Arrays.copyOf(values, size);
        }
    }

    private static final class IntArrayCodec extends PrimitiveArrayCodec {
        IntArrayCodec(MorphiaDatastore datastore) {
            super(datastore, int.class, IntegerCodec.class);
        }

        @Override
        void write(BsonWriter writer, Object value) {
            for (int element : (int[]) value) {
                writer.writeInt32(element);
            }
        }

        @Override
        Object read(BsonReader reader, Codec codec, DecoderContext decoderContext) {
            int[] values = new int[capacity()];
            int size = 0;
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (size == values.length) {
                    values = Arrays.copyOf(values, size * 2);
                }
                values[size++] = reader.getCurrentBsonType() == BsonType.INT32
                        ? reader.readInt32()
                        : (Integer) codec.decode(reader, decoderContext);
            }
            return exact(values.length, size) ? values : Arrays.copyOf(values, size);
        }
    }

    private static final class LongArrayCodec extends PrimitiveArrayCodec {
        LongArrayCodec(MorphiaDatastore datastore) {
            super(datastore, long.class, LongCodec.class);
        }

        @Override
        void write(BsonWriter writer, Object value) {
            for (long element : (long[]) value) {
                writer.writeInt64(element);
            }
        }

        @Override
        Object read(BsonReader reader, Codec codec, DecoderContext decoderContext) {
            long[] values = new long[capacity()];
            int size = 0;
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (size == values.length) {
                    values = Arrays.copyOf(values, size * 2);
                }
                values[size++] = reader.getCurrentBsonType() == BsonType.INT64
                        ? reader.readInt64()
                        : (Long) codec.decode(reader, decoderContext);
            }
            return exact(values.length, size) ? values : Arrays.copyOf(values, size);
        }
    }

    private static final class ShortArrayCodec extends PrimitiveArrayCodec {
        ShortArrayCodec(MorphiaDatastore datastore) {
            super(datastore, short.class, ShortCodec.class);
        }

        @Override
        void write(BsonWriter writer, Object value) {
            for (short element : (short[]) value) {
                writer.writeInt32(element);
            }
        }

        @Override
        Object read(BsonReader reader, Codec codec, DecoderContext decoderContext) {
            short[] values = new short[capacity()];
            int size = 0;
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (size == values.length) {
                    values = Arrays.copyOf(values, size * 2);
                }
                short element;
                if (reader.getCurrentBsonType() == BsonType.INT32) {
                    // the range check ShortCodec makes
                    int value = reader.readInt32();
                    if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                        throw new BsonInvalidOperationException(format("%s can not be converted into a Short.", value));
                    }
                    element = (short) value;
                } else {
                    element = (Short) codec.decode(reader, decoderContext);
                }
                values[size++] = element;
            }
            return exact(values.length, size) ? values : Arrays.copyOf(values, size);
        }
    }

    private static final class BooleanArrayCodec extends PrimitiveArrayCodec {
        BooleanArrayCodec(MorphiaDatastore datastore) {
            super(datastore, boolean.class, BooleanCodec.class);
        }

        @Override
        void write(BsonWriter writer, Object value) {
            for (boolean element : (boolean[]) value) {
                writer.writeBoolean(element);
            }
        }

        @Override
        Object read(BsonReader reader, Codec codec, DecoderContext decoderContext) {
            boolean[] values = new boolean[capacity()];
            int size = 0;
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (size == values.length) {
                    values = Arrays.copyOf(values, size * 2);
                }
                // BooleanCodec reads every element with readBoolean
                values[size++] = reader.readBoolean();
            }
            return exact(values.length, size) ? values : Arrays.copyOf(values, size);
        }
    }

    private static final class CharArrayCodec extends PrimitiveArrayCodec {
        CharArrayCodec(MorphiaDatastore datastore) {
            super(datastore, char.class, CharacterCodec.class);
        }

        @Override
        void write(BsonWriter writer, Object value) {
            for (char element : (char[]) value) {
                writer.writeString(String.valueOf(element));
            }
        }

        @Override
        Object read(BsonReader reader, Codec codec, DecoderContext decoderContext) {
            char[] values = new char[capacity()];
            int size = 0;
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                if (size == values.length) {
                    values = Arrays.copyOf(values, size * 2);
                }
                // CharacterCodec's string checks; Character.valueOf caches the ASCII range, so this rarely allocates
                values[size++] = (Character) codec.decode(reader, decoderContext);
            }
            return exact(values.length, size) ? values : Arrays.copyOf(values, size);
        }
    }
}
