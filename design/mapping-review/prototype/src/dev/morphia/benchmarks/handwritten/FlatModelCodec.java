package dev.morphia.benchmarks.handwritten;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Transient;

import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

/**
 * A simulation of options O2/O3: no {@code EntityModel}/{@code PropertyModel} at runtime, only a flat, immutable,
 * precomputed form (arrays of names, {@link MethodHandle} accessors adapted to {@code Object}, codecs resolved once,
 * a name-to-slot map) driven by ONE shared interpretive loop. It removes every indirection the model adds except the
 * interpretation itself: megamorphic call sites across properties, boxing of primitives, and non-constant method
 * handles. Comparing it with the hand-written codecs separates "flatten the model" from "generate code".
 *
 * @param <T> the entity type
 */
public final class FlatModelCodec<T> implements Codec<T> {
    private static final MethodType GETTER = MethodType.methodType(Object.class, Object.class);
    private static final MethodType SETTER = MethodType.methodType(void.class, Object.class, Object.class);

    private static final class Slot {
        final String name;
        final MethodHandle getter;
        final MethodHandle setter;
        final Codec<Object> codec;
        final boolean container;

        Slot(String name, MethodHandle getter, MethodHandle setter, Codec<Object> codec, boolean container) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.codec = codec;
            this.container = container;
        }
    }

    private final Class<T> type;
    private final CodecSupport.Context context;
    private final MethodHandle constructor;
    private final String discriminatorKey;
    private final String discriminator;
    private final Slot id;
    private final Slot[] slots;
    private final Map<String, Slot> byName = new HashMap<>();
    private final boolean storeNulls;
    private final boolean storeEmpties;

    /**
     * @param codecFor resolves a codec for a field once, at build time (embedded types resolve to other flat codecs)
     */
    @SuppressWarnings("unchecked")
    public FlatModelCodec(Class<T> type, CodecSupport.Context context, Function<Field, Codec<?>> codecFor) {
        this.type = type;
        this.context = context;
        this.storeNulls = context.storeNulls;
        this.storeEmpties = context.storeEmpties;
        this.discriminatorKey = context.mapper.getConfig().discriminatorKey();
        this.discriminator = type.getSimpleName();
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
            constructor = lookup.findConstructor(type, MethodType.methodType(void.class))
                    .asType(MethodType.methodType(Object.class));
            Slot idSlot = null;
            List<Slot> list = new ArrayList<>();
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                MethodHandles.Lookup owner = MethodHandles.privateLookupIn(c, MethodHandles.lookup());
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())
                            || field.isAnnotationPresent(Transient.class)) {
                        continue;
                    }
                    Property property = field.getAnnotation(Property.class);
                    boolean isId = field.isAnnotationPresent(Id.class);
                    String name = isId ? "_id" : property != null ? property.value() : field.getName();
                    Slot slot = new Slot(name,
                            owner.unreflectGetter(field).asType(GETTER),
                            owner.unreflectSetter(field).asType(SETTER),
                            (Codec<Object>) codecFor.apply(field),
                            Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()));
                    byName.put(name, slot);
                    if (isId) {
                        idSlot = slot;
                    } else {
                        list.add(slot);
                    }
                }
            }
            this.id = idSlot;
            this.slots = list.toArray(new Slot[0]);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return type;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void encode(BsonWriter writer, T value, EncoderContext ctx) {
        if (value.getClass() != type) {
            ((Codec) context.registry.get(value.getClass())).encode(writer, value, ctx);
            return;
        }
        try {
            writer.writeStartDocument();
            if (id != null) {
                Object idValue = (Object) id.getter.invokeExact((Object) value);
                if (idValue == null && ctx.isEncodingCollectibleDocument()) {
                    idValue = new ObjectId();
                    id.setter.invokeExact((Object) value, idValue);
                }
                write(writer, id, idValue);
            }
            writer.writeString(discriminatorKey, discriminator);
            for (Slot slot : slots) {
                write(writer, slot, (Object) slot.getter.invokeExact((Object) value));
            }
            writer.writeEndDocument();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private void write(BsonWriter writer, Slot slot, Object value) {
        if (value == null) {
            if (storeNulls) {
                writer.writeNull(slot.name);
            }
            return;
        }
        if (!storeEmpties && slot.container
                && (value instanceof Collection<?> c && c.isEmpty() || value instanceof Map<?, ?> m && m.isEmpty())) {
            return;
        }
        writer.writeName(slot.name);
        slot.codec.encode(writer, value, CodecSupport.CHILD_ENCODER);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T decode(BsonReader reader, DecoderContext ctx) {
        try {
            BsonReaderMark mark = reader.getMark();
            Object entity = (Object) constructor.invokeExact();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                    continue;
                }
                if (discriminatorKey.equals(name)) {
                    String value = reader.readString();
                    if (!discriminator.equals(value)) {
                        mark.reset();
                        return CodecSupport.<T> subtypeCodec(context, value).decode(reader, ctx);
                    }
                    continue;
                }
                Slot slot = byName.get(name);
                if (slot == null) {
                    reader.skipValue();
                } else {
                    slot.setter.invokeExact(entity, (Object) slot.codec.decode(reader, ctx));
                }
            }
            reader.readEndDocument();
            return (T) entity;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
