package dev.morphia.mapping.codec.pojo;

import java.util.Arrays;
import java.util.List;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.codecs.Codec;

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

    @SuppressWarnings("unchecked")
    CodecPlan(EntityModel model) {
        this.model = model;
        List<PropertyModel> list = model.getProperties();
        properties = list.toArray(new PropertyModel[0]);
        codecs = new Codec[properties.length];
        for (int slot = 0; slot < properties.length; slot++) {
            PropertyModel property = properties[slot];
            if (property.slot() != slot) {
                throw new IllegalStateException("%s is in slot %d of %s but records slot %d".formatted(property.getName(), slot,
                        model.getType().getName(), property.slot()));
            }
            codecs[slot] = (Codec<? super Object>) property.getCodec();
        }
        PropertyModel idProperty = model.getIdProperty();
        encodeOrder = Arrays.stream(properties)
                .filter(property -> property != idProperty)
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
     * @return the properties to encode after the id, in order. Callers must not modify the array.
     */
    PropertyModel[] encodeOrder() {
        return encodeOrder;
    }
}
