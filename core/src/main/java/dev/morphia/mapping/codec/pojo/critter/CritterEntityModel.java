package dev.morphia.mapping.codec.pojo.critter;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import dev.morphia.annotations.IdField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.ArrayAccessor;
import dev.morphia.mapping.codec.Conversions;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.MethodAccessor;
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

    /**
     * Gives a property a reflective accessor for a field the generated accessors can't reach, as the reflective mapper
     * would.
     *
     * @param property the property
     * @param type     the class declaring the field, or, for an {@code @ExternalEntity} stand-in, the target whose
     *                 hierarchy declares it
     * @param name     the field's name
     */
    @SuppressWarnings("unused")
    protected final void fieldAccessor(PropertyModel property, Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    property.accessor(new FieldAccessor(field));
                    return;
                }
            }
        }
        throw new IllegalStateException("No field '%s' in %s".formatted(name, type.getName()));
    }

    /**
     * Gives a property a reflective accessor for a getter and setter the generated accessors can't call, as the reflective
     * mapper would.
     *
     * @param property         the property
     * @param type             the class declaring the methods, or, for an {@code @ExternalEntity} stand-in, the target
     *                         whose hierarchy declares them
     * @param getter           the getter's name
     * @param getterDescriptor the getter's descriptor
     * @param setter           the setter's name
     * @param setterDescriptor the setter's descriptor
     */
    @SuppressWarnings("unused")
    protected final void methodAccessor(PropertyModel property, Class<?> type, String getter, String getterDescriptor,
            String setter, String setterDescriptor) {
        property.accessor(new MethodAccessor(method(type, getter, getterDescriptor), method(type, setter, setterDescriptor)));
    }

    private static Method method(Class<?> type, String name, String descriptor) {
        Class<?>[] parameters = MethodType.fromMethodDescriptorString(descriptor, type.getClassLoader()).parameterArray();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && !method.isBridge()
                        && Arrays.equals(method.getParameterTypes(), parameters)) {
                    return method;
                }
            }
        }
        throw new IllegalStateException("No method %s%s in %s".formatted(name, descriptor, type.getName()));
    }
}
