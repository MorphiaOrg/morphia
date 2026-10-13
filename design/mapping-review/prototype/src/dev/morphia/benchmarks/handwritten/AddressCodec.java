package dev.morphia.benchmarks.handwritten;

import java.lang.invoke.VarHandle;

import dev.morphia.benchmarks.models.Address;

import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Generated-style codec for the embedded {@link Address} (no {@code _id}; Morphia still writes {@code _t} because
 * {@code @Entity#useDiscriminator} defaults to true).
 */
public final class AddressCodec implements Codec<Address> {
    private static final VarHandle STREET = CodecSupport.field(Address.class, "street", String.class);
    private static final VarHandle CITY = CodecSupport.field(Address.class, "city", String.class);
    private static final VarHandle POST_CODE = CodecSupport.field(Address.class, "postCode", String.class);
    private static final VarHandle COUNTRY = CodecSupport.field(Address.class, "country", String.class);
    private static final String DISCRIMINATOR = "Address";

    private final CodecSupport.Context context;
    private final boolean storeNulls;

    public AddressCodec(CodecSupport.Context context) {
        this.context = context;
        this.storeNulls = context.storeNulls;
    }

    @Override
    public Class<Address> getEncoderClass() {
        return Address.class;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void encode(BsonWriter writer, Address value, EncoderContext ctx) {
        if (value.getClass() != Address.class) {
            ((Codec) context.registry.get(value.getClass())).encode(writer, value, ctx);
            return;
        }
        writer.writeStartDocument();
        writer.writeString("_t", DISCRIMINATOR);
        writeString(writer, "street_name", (String) STREET.get(value));
        writeString(writer, "city", (String) CITY.get(value));
        writeString(writer, "postCode", (String) POST_CODE.get(value));
        writeString(writer, "country", (String) COUNTRY.get(value));
        writer.writeEndDocument();
    }

    private void writeString(BsonWriter writer, String name, String value) {
        if (value != null) {
            writer.writeString(name, value);
        } else if (storeNulls) {
            writer.writeNull(name);
        }
    }

    @Override
    public Address decode(BsonReader reader, DecoderContext ctx) {
        BsonReaderMark mark = reader.getMark();
        Address entity = new Address();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if (reader.getCurrentBsonType() == BsonType.NULL) {
                reader.readNull();
                continue;
            }
            switch (name) {
                case "_t" -> {
                    String discriminator = reader.readString();
                    if (!DISCRIMINATOR.equals(discriminator)) {
                        mark.reset();
                        return CodecSupport.<Address> subtypeCodec(context, discriminator).decode(reader, ctx);
                    }
                }
                case "street_name" -> STREET.set(entity, CodecSupport.readStringValue(context, reader, ctx));
                case "city" -> CITY.set(entity, CodecSupport.readStringValue(context, reader, ctx));
                case "postCode" -> POST_CODE.set(entity, CodecSupport.readStringValue(context, reader, ctx));
                case "country" -> COUNTRY.set(entity, CodecSupport.readStringValue(context, reader, ctx));
                default -> reader.skipValue();
            }
        }
        reader.readEndDocument();
        return entity;
    }
}
