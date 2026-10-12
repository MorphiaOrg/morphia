package dev.morphia.benchmarks.handwritten;

import java.lang.invoke.VarHandle;
import java.util.List;

import dev.morphia.benchmarks.models.Address;
import dev.morphia.benchmarks.models.Profile;

import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Generated-style codec for the embedded {@link Profile}; calls {@link AddressCodec} directly (no registry lookup, no
 * discriminator pre-scan) and writes/reads {@code List<String>} inline.
 */
public final class ProfileCodec implements Codec<Profile> {
    private static final VarHandle BIO = CodecSupport.field(Profile.class, "bio", String.class);
    private static final VarHandle ADDRESS = CodecSupport.field(Profile.class, "address", Address.class);
    private static final VarHandle INTERESTS = CodecSupport.field(Profile.class, "interests", List.class);
    private static final String DISCRIMINATOR = "Profile";

    private final CodecSupport.Context context;
    private final boolean storeNulls;
    private final boolean storeEmpties;
    private final AddressCodec addressCodec;

    public ProfileCodec(CodecSupport.Context context, AddressCodec addressCodec) {
        this.context = context;
        this.storeNulls = context.storeNulls;
        this.storeEmpties = context.storeEmpties;
        this.addressCodec = addressCodec;
    }

    @Override
    public Class<Profile> getEncoderClass() {
        return Profile.class;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void encode(BsonWriter writer, Profile value, EncoderContext ctx) {
        if (value.getClass() != Profile.class) {
            ((Codec) context.registry.get(value.getClass())).encode(writer, value, ctx);
            return;
        }
        writer.writeStartDocument();
        writer.writeString("_t", DISCRIMINATOR);
        String bio = (String) BIO.get(value);
        if (bio != null) {
            writer.writeString("bio", bio);
        } else if (storeNulls) {
            writer.writeNull("bio");
        }
        Address address = (Address) ADDRESS.get(value);
        if (address != null) {
            writer.writeName("address");
            addressCodec.encode(writer, address, CodecSupport.CHILD_ENCODER);
        } else if (storeNulls) {
            writer.writeNull("address");
        }
        @SuppressWarnings("unchecked")
        List<String> interests = (List<String>) INTERESTS.get(value);
        if (interests == null) {
            if (storeNulls) {
                writer.writeNull("interests");
            }
        } else if (storeEmpties || !interests.isEmpty()) {
            writer.writeName("interests");
            CodecSupport.writeStringList(writer, interests);
        }
        writer.writeEndDocument();
    }

    @Override
    public Profile decode(BsonReader reader, DecoderContext ctx) {
        BsonReaderMark mark = reader.getMark();
        Profile entity = new Profile();
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
                        return CodecSupport.<Profile> subtypeCodec(context, discriminator).decode(reader, ctx);
                    }
                }
                case "bio" -> BIO.set(entity, CodecSupport.readStringValue(context, reader, ctx));
                case "address" -> ADDRESS.set(entity, addressCodec.decode(reader, ctx));
                case "interests" -> INTERESTS.set(entity, CodecSupport.readStringList(context, reader, ctx));
                default -> reader.skipValue();
            }
        }
        reader.readEndDocument();
        return entity;
    }
}
