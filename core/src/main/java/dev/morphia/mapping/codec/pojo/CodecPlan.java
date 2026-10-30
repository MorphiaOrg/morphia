package dev.morphia.mapping.codec.pojo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.LoadOnly;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.EnumCodec;
import dev.morphia.mapping.codec.MorphiaDateCodec;
import dev.morphia.mapping.codec.MorphiaLocalDateTimeCodec;

import org.bson.BsonType;
import org.bson.codecs.BigDecimalCodec;
import org.bson.codecs.BooleanCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DateCodec;
import org.bson.codecs.Decimal128Codec;
import org.bson.codecs.DoubleCodec;
import org.bson.codecs.IntegerCodec;
import org.bson.codecs.LongCodec;
import org.bson.codecs.ObjectIdCodec;
import org.bson.codecs.StringCodec;

import static java.util.Map.entry;

/**
 * The per-codec state a {@link MorphiaCodec} reads while encoding and decoding, built once when the codec is created.
 * Properties are addressed by their {@link PropertyModel#slot() slot}, their position in their entity model, so a
 * lookup is an array index.
 * <p>
 * It holds what belongs to one codec rather than to the shared mapping model: the codecs resolved for this datastore
 * and the order properties are encoded in. Later work adds more per-slot data here.
 * </p>
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class CodecPlan {
    private final EntityModel model;
    private final PropertyModel[] properties;
    private final Codec<? super Object>[] codecs;
    private final PropertyModel[] encodeOrder;
    // per slot, a bitmask of the BSON types (by ordinal) the slot's codec reads without being able to fail part-way
    private final int[] directBsonTypes;

    /**
     * Codecs that read the listed BSON types directly: they either read the value and return it, or throw before
     * reading anything. A property whose codec reads the current type directly can be decoded without a reader mark,
     * since there's never a partly consumed value to rewind. Keyed on the exact codec class, so subclasses and user
     * codecs keep the mark. Types a codec reads and then may reject, such as an {@code IntegerCodec} reading an INT64
     * that doesn't fit, aren't listed.
     */
    static final Map<Class<?>, Integer> DIRECT_BSON_TYPES = Map.ofEntries(
            entry(StringCodec.class, mask(BsonType.STRING, BsonType.SYMBOL)),
            entry(ObjectIdCodec.class, mask(BsonType.OBJECT_ID)),
            entry(IntegerCodec.class, mask(BsonType.INT32)),
            entry(LongCodec.class, mask(BsonType.INT32, BsonType.INT64)),
            entry(DoubleCodec.class, mask(BsonType.DOUBLE, BsonType.INT32)),
            entry(BooleanCodec.class, mask(BsonType.BOOLEAN)),
            entry(Decimal128Codec.class, mask(BsonType.DECIMAL128)),
            entry(BigDecimalCodec.class, mask(BsonType.DECIMAL128)),
            entry(DateCodec.class, mask(BsonType.DATE_TIME)),
            entry(EnumCodec.class, mask(BsonType.STRING)),
            entry(MorphiaLocalDateTimeCodec.class, mask(BsonType.DATE_TIME, BsonType.INT64)),
            entry(MorphiaDateCodec.class, mask(BsonType.DATE_TIME)));

    @SuppressWarnings("unchecked")
    CodecPlan(EntityModel model, Codec<?>[] propertyCodecs) {
        this.model = model;
        List<PropertyModel> list = model.getProperties();
        properties = list.toArray(new PropertyModel[0]);
        codecs = new Codec[properties.length];
        directBsonTypes = new int[properties.length];
        for (int slot = 0; slot < properties.length; slot++) {
            PropertyModel property = properties[slot];
            if (property.slot() != slot) {
                throw new IllegalStateException("%s is in slot %d of %s but records slot %d".formatted(property.getName(), slot,
                        model.getType().getName(), property.slot()));
            }
            codecs[slot] = (Codec<? super Object>) propertyCodecs[slot];
            directBsonTypes[slot] = codecs[slot] != null ? DIRECT_BSON_TYPES.getOrDefault(codecs[slot].getClass(), 0) : 0;
        }
        PropertyModel idProperty = model.getIdProperty();
        // the id is written first, separately, and @LoadOnly properties are never written by any encoder
        encodeOrder = Arrays.stream(properties)
                .filter(property -> property != idProperty && !property.hasAnnotation(LoadOnly.class))
                .toArray(PropertyModel[]::new);
    }

    /**
     * @return the entity model this plan is for
     */
    public EntityModel model() {
        return model;
    }

    /**
     * @param property a property of this plan's model
     * @return the codec for the property, or null if none was resolved
     */
    @Nullable
    public Codec<? super Object> codec(PropertyModel property) {
        int slot = property.slot();
        if (slot < 0 || slot >= properties.length || properties[slot] != property) {
            throw new IllegalArgumentException("%s isn't a property of %s".formatted(property, model.getType().getName()));
        }
        return codecs[slot];
    }

    /**
     * @param property a property of this plan's model
     * @param type     the BSON type about to be read for it
     * @return true if the property's codec reads that type without being able to fail part-way, so decoding it needs no
     *         reader mark to fall back to
     */
    public boolean readsDirectly(PropertyModel property, BsonType type) {
        return (directBsonTypes[property.slot()] & (1 << type.ordinal())) != 0;
    }

    private static int mask(BsonType... types) {
        int mask = 0;
        for (BsonType type : types) {
            mask |= 1 << type.ordinal();
        }
        return mask;
    }

    /**
     * @return the properties to encode after the id, in order, without {@code @LoadOnly} properties. Callers must not
     *         modify the array.
     */
    PropertyModel[] encodeOrder() {
        return encodeOrder;
    }
}
