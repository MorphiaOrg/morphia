package dev.morphia.mapping.codec.pojo.critter;

import java.lang.annotation.Annotation;

import dev.morphia.annotations.IdField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.ArrayAccessor;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.MorphiaPropertySerialization;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.conventions.MorphiaConvention;

/**
 * The base of critter's generated entity models. A generated model's constructor configures it from constants, as the
 * reflective mapper configures an {@link EntityModel}, and builds plain {@link PropertyModel}s; it overrides nothing.
 *
 * @hidden
 * @morphia.internal
 */
public abstract class CritterEntityModel extends EntityModel {
    private final Conversions conversions;

    /**
     * @param mapper the mapper the model is built for
     * @param type   the entity type
     */
    public CritterEntityModel(Mapper mapper, Class<?> type) {
        super(type);
        conversions = mapper.getConversions();
    }

    /**
     * Sets up {@link MorphiaPropertySerialization} and the mapper's {@link dev.morphia.mapping.codec.Conversions} on every
     * property model, marks the {@code @IdField} property as the id, and applies the mapper's ServiceLoader conventions.
     * Must be called at the end of the generated subclass constructor, after all
     * properties have been added via {@code addProperty()}.
     *
     * @param mapper the mapper whose configuration governs serialization behaviour
     */
    @SuppressWarnings("unused")
    protected final void configureProperties(Mapper mapper) {
        for (PropertyModel property : getProperties()) {
            property.serialization(new MorphiaPropertySerialization(mapper.getConfig(), property));
            // Without this, PropertyModel#setValue builds a new Conversions registry on every call.
            property.conversions(mapper.getConversions());
        }
        // the generator already maps the @IdField property to _id; mark it as the id
        IdField idField = getAnnotation(IdField.class);
        if (idField != null && getIdProperty() == null) {
            setIdProperty(getPropertyByName(idField.value()));
        }
        // as for reflective models, the ServiceLoader conventions run last, here when the model is loaded: they can depend
        // on the runtime classpath and runtime state, e.g. Kotlin's delegated properties
        for (MorphiaConvention convention : mapper.getConventions()) {
            convention.apply(mapper, this);
        }
        initializeListeners();
    }

    /**
     * Wraps an array property's accessor to convert decoded values to its array type, as the reflective mapper does.
     *
     * @param property      the property
     * @param componentType the component type of the property's erased type
     */
    @SuppressWarnings("unused")
    protected final void arrayAccessor(PropertyModel property, Class<?> componentType) {
        property.accessor(new ArrayAccessor(property.getAccessor(), componentType, conversions));
    }

    /**
     * Adds the annotations of one of the generated subclass's fields to a property: the property's annotations other
     * than Morphia's, which have no builders to construct them with.
     *
     * @param property the property
     * @param holder   the name of the field carrying the annotations
     */
    @SuppressWarnings("unused")
    protected final void foreignAnnotations(PropertyModel property, String holder) {
        try {
            for (Annotation annotation : getClass().getDeclaredField(holder).getDeclaredAnnotations()) {
                property.annotation(annotation);
            }
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
