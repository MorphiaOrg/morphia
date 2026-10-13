package dev.morphia.benchmarks.handwritten;

import java.lang.invoke.VarHandle;

import dev.morphia.benchmarks.models.Address;
import dev.morphia.benchmarks.models.NestedEntity;
import dev.morphia.benchmarks.models.Profile;

import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

/**
 * Generated-style codec for {@link NestedEntity}. Embedded values go straight to the embedded types' generated codecs,
 * which are held in final fields: no registry lookup, no {@code encodeWithChildContext} indirection, and no discriminator
 * pre-scan of every embedded document.
 */
public final class NestedEntityCodec implements CollectibleCodec<NestedEntity> {
    private static final VarHandle ID = CodecSupport.field(NestedEntity.class, "id", ObjectId.class);
    private static final VarHandle NAME = CodecSupport.field(NestedEntity.class, "name", String.class);
    private static final VarHandle HOME = CodecSupport.field(NestedEntity.class, "home", Address.class);
    private static final VarHandle WORK = CodecSupport.field(NestedEntity.class, "work", Address.class);
    private static final VarHandle PROFILE = CodecSupport.field(NestedEntity.class, "profile", Profile.class);
    private static final String DISCRIMINATOR = "NestedEntity";

    private final CodecSupport.Context context;
    private final boolean storeNulls;
    private final AddressCodec addressCodec;
    private final ProfileCodec profileCodec;

    public NestedEntityCodec(CodecSupport.Context context, AddressCodec addressCodec, ProfileCodec profileCodec) {
        this.context = context;
        this.storeNulls = context.storeNulls;
        this.addressCodec = addressCodec;
        this.profileCodec = profileCodec;
    }

    @Override
    public Class<NestedEntity> getEncoderClass() {
        return NestedEntity.class;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void encode(BsonWriter writer, NestedEntity value, EncoderContext ctx) {
        if (value.getClass() != NestedEntity.class) {
            ((Codec) context.registry.get(value.getClass())).encode(writer, value, ctx);
            return;
        }
        writer.writeStartDocument();
        ObjectId id = (ObjectId) ID.get(value);
        if (id == null && ctx.isEncodingCollectibleDocument()) {
            id = new ObjectId();
            ID.set(value, id);
        }
        if (id != null) {
            writer.writeObjectId("_id", id);
        } else if (storeNulls) {
            writer.writeNull("_id");
        }
        writer.writeString("_t", DISCRIMINATOR);
        String name = (String) NAME.get(value);
        if (name != null) {
            writer.writeString("name", name);
        } else if (storeNulls) {
            writer.writeNull("name");
        }
        writeAddress(writer, "home", (Address) HOME.get(value));
        writeAddress(writer, "work", (Address) WORK.get(value));
        Profile profile = (Profile) PROFILE.get(value);
        if (profile != null) {
            writer.writeName("profile");
            profileCodec.encode(writer, profile, CodecSupport.CHILD_ENCODER);
        } else if (storeNulls) {
            writer.writeNull("profile");
        }
        writer.writeEndDocument();
    }

    private void writeAddress(BsonWriter writer, String name, Address address) {
        if (address != null) {
            writer.writeName(name);
            addressCodec.encode(writer, address, CodecSupport.CHILD_ENCODER);
        } else if (storeNulls) {
            writer.writeNull(name);
        }
    }

    @Override
    public NestedEntity decode(BsonReader reader, DecoderContext ctx) {
        BsonReaderMark mark = reader.getMark();
        NestedEntity entity = new NestedEntity();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            BsonType type = reader.getCurrentBsonType();
            if (type == BsonType.NULL) {
                reader.readNull();
                continue;
            }
            switch (name) {
                case "_id" -> ID.set(entity, type == BsonType.OBJECT_ID ? reader.readObjectId()
                        : (ObjectId) CodecSupport.slowRead(context, reader, ctx, ObjectId.class));
                case "_t" -> {
                    String discriminator = reader.readString();
                    if (!DISCRIMINATOR.equals(discriminator)) {
                        mark.reset();
                        return CodecSupport.<NestedEntity> subtypeCodec(context, discriminator).decode(reader, ctx);
                    }
                }
                case "name" -> NAME.set(entity, CodecSupport.readStringValue(context, reader, ctx));
                case "home" -> HOME.set(entity, addressCodec.decode(reader, ctx));
                case "work" -> WORK.set(entity, addressCodec.decode(reader, ctx));
                case "profile" -> PROFILE.set(entity, profileCodec.decode(reader, ctx));
                default -> reader.skipValue();
            }
        }
        reader.readEndDocument();
        return entity;
    }

    @Override
    public NestedEntity generateIdIfAbsentFromDocument(NestedEntity document) {
        if ((ObjectId) ID.get(document) == null) {
            ID.set(document, new ObjectId());
        }
        return document;
    }

    @Override
    public boolean documentHasId(NestedEntity document) {
        return (ObjectId) ID.get(document) != null;
    }

    @Override
    public BsonValue getDocumentId(NestedEntity document) {
        throw new UnsupportedOperationException();
    }
}
