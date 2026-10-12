package dev.morphia.benchmarks.handwritten;

import java.lang.invoke.VarHandle;
import java.time.LocalDateTime;

import dev.morphia.benchmarks.models.SimpleEntity;
import dev.morphia.benchmarks.models.Status;

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
 * What a generated codec for {@link SimpleEntity} could look like: straight-line writes with constant names, a string
 * switch for reads, primitives never boxed, every call site monomorphic, no per-property model objects at runtime.
 * <p>
 * Field access uses {@code static final} {@link VarHandle}s because the plain benchmark classes have private fields and no
 * setters; the JIT constant-folds these, so after warm-up this is equivalent to the direct {@code getfield}/{@code putfield}
 * a hidden-nestmate (runtime) or a woven {@code __readX/__writeX} (AOT) codec would use.
 * <p>
 * Matches Morphia's defaults for this model: {@code _id} first, then the {@code _t} discriminator, then the properties in
 * declaration order; null properties are skipped unless {@code storeNulls}; an absent ObjectId {@code _id} is generated
 * when encoding a collectible document.
 */
public final class SimpleEntityCodec implements CollectibleCodec<SimpleEntity> {
    private static final VarHandle ID = CodecSupport.field(SimpleEntity.class, "id", ObjectId.class);
    private static final VarHandle NAME = CodecSupport.field(SimpleEntity.class, "name", String.class);
    private static final VarHandle AGE = CodecSupport.field(SimpleEntity.class, "age", int.class);
    private static final VarHandle COUNT = CodecSupport.field(SimpleEntity.class, "count", long.class);
    private static final VarHandle SCORE = CodecSupport.field(SimpleEntity.class, "score", double.class);
    private static final VarHandle ACTIVE = CodecSupport.field(SimpleEntity.class, "active", boolean.class);
    private static final VarHandle STATUS = CodecSupport.field(SimpleEntity.class, "status", Status.class);
    private static final VarHandle CREATED = CodecSupport.field(SimpleEntity.class, "created", LocalDateTime.class);

    private static final String DISCRIMINATOR_KEY = "_t";
    private static final String DISCRIMINATOR = "SimpleEntity";
    private static final EncoderContext CHILD = EncoderContext.builder().build();

    private final CodecSupport.Context context;
    private final boolean storeNulls;
    private final Codec<LocalDateTime> localDateTimeCodec;

    public SimpleEntityCodec(CodecSupport.Context context) {
        this.context = context;
        this.storeNulls = context.storeNulls;
        this.localDateTimeCodec = context.registry.get(LocalDateTime.class);
    }

    @Override
    public Class<SimpleEntity> getEncoderClass() {
        return SimpleEntity.class;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void encode(BsonWriter writer, SimpleEntity value, EncoderContext ctx) {
        if (value.getClass() != SimpleEntity.class) {
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

        writer.writeString(DISCRIMINATOR_KEY, DISCRIMINATOR);

        String name = (String) NAME.get(value);
        if (name != null) {
            writer.writeString("name", name);
        } else if (storeNulls) {
            writer.writeNull("name");
        }
        writer.writeInt32("age", (int) AGE.get(value));
        writer.writeInt64("count", (long) COUNT.get(value));
        writer.writeDouble("score", (double) SCORE.get(value));
        writer.writeBoolean("active", (boolean) ACTIVE.get(value));
        Status status = (Status) STATUS.get(value);
        if (status != null) {
            writer.writeString("status", status.name());
        } else if (storeNulls) {
            writer.writeNull("status");
        }
        LocalDateTime created = (LocalDateTime) CREATED.get(value);
        if (created != null) {
            writer.writeName("created");
            localDateTimeCodec.encode(writer, created, CHILD);
        } else if (storeNulls) {
            writer.writeNull("created");
        }

        writer.writeEndDocument();
    }

    @Override
    public SimpleEntity decode(BsonReader reader, DecoderContext ctx) {
        BsonReaderMark mark = reader.getMark();
        SimpleEntity entity = new SimpleEntity();
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
                case DISCRIMINATOR_KEY -> {
                    String discriminator = reader.readString();
                    if (!DISCRIMINATOR.equals(discriminator)) {
                        mark.reset();
                        return CodecSupport.<SimpleEntity> subtypeCodec(context, discriminator).decode(reader, ctx);
                    }
                }
                case "name" -> NAME.set(entity, type == BsonType.STRING ? reader.readString()
                        : (String) CodecSupport.slowRead(context, reader, ctx, String.class));
                case "age" -> AGE.set(entity, CodecSupport.readInt(reader, ctx));
                case "count" -> COUNT.set(entity, CodecSupport.readLong(reader, ctx));
                case "score" -> SCORE.set(entity, CodecSupport.readDouble(reader, ctx));
                case "active" -> ACTIVE.set(entity, CodecSupport.readBoolean(reader, ctx));
                case "status" -> STATUS.set(entity, type == BsonType.STRING ? Status.valueOf(reader.readString())
                        : (Status) CodecSupport.slowRead(context, reader, ctx, Status.class));
                case "created" -> CREATED.set(entity, localDateTimeCodec.decode(reader, ctx));
                default -> reader.skipValue();
            }
        }
        reader.readEndDocument();
        return entity;
    }

    @Override
    public SimpleEntity generateIdIfAbsentFromDocument(SimpleEntity document) {
        if ((ObjectId) ID.get(document) == null) {
            ID.set(document, new ObjectId());
        }
        return document;
    }

    @Override
    public boolean documentHasId(SimpleEntity document) {
        return (ObjectId) ID.get(document) != null;
    }

    @Override
    public BsonValue getDocumentId(SimpleEntity document) {
        throw new UnsupportedOperationException();
    }
}
