package dev.morphia.mapping.codec.pojo.critter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import dev.morphia.annotations.IdField;
import dev.morphia.config.MorphiaConfig;
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
    /**
     * The version of Morphia that's running. A pre-generated model is used only with the version it was generated with.
     */
    public static final String MORPHIA_VERSION = morphiaVersion();

    /**
     * The name of a pre-generated model's constant holding the {@link #MORPHIA_VERSION} it was generated with.
     */
    public static final String MORPHIA_VERSION_FIELD = "__morphiaVersion";

    /**
     * The name of a pre-generated model's constant holding the {@link #generationKey(MorphiaConfig) generation key} of the
     * configuration it was generated with.
     */
    public static final String GENERATION_KEY = "__generationKey";

    /**
     * The name of a pre-generated model's constant holding the ID of the critter-maven run that generated it.
     */
    public static final String BUILD_ID = "__buildId";

    /**
     * The name of a pre-generated model's constant listing, separated by commas, the classes woven for it: the entity and
     * the superclasses whose fields are woven into them.
     */
    public static final String WOVEN_CLASSES = "__wovenClasses";

    /**
     * The prefix of the private synthetic field critter-maven adds to each class it weaves, named for the run's ID.
     */
    public static final String BUILD_STAMP = "__morphiaBuild$";

    // the languages whose case conversion differs from the root locale's: the dotted and dotless i, and Lithuanian's dot above
    private static final Set<String> CASE_RULE_LANGUAGES = Set.of("tr", "az", "lt");

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
     * @param type    the entity
     * @param woven   the name of a class woven with it: the entity or one of its superclasses
     * @param buildId the ID of the build that generated the entity's model
     * @return true if the class is stamped with that build's ID, which it isn't if it was compiled again, or woven by
     *         another build, after the model was generated
     */
    public static boolean isWovenBy(Class<?> type, String woven, String buildId) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (current.getName().equals(woven)) {
                try {
                    // only looked up, so the field's access doesn't matter, and the class isn't initialized
                    current.getDeclaredField(BUILD_STAMP + buildId);
                    return true;
                } catch (NoSuchFieldException e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Describes the configuration settings generation bakes into a model: the collection and property naming, the
     * discriminator function and key, property discovery and the property annotation providers, and the case rules of the
     * default locale, which the built-in strategies convert case with. Strategies are identified by class, which tells two
     * configurations apart only if every strategy is one of Morphia's stateless built-ins.
     *
     * @param config the configuration
     * @return the key
     */
    public static String generationKey(MorphiaConfig config) {
        StringBuilder key = new StringBuilder(256)
                .append("collectionNaming=").append(config.collectionNaming().getClass().getName())
                .append(", propertyNaming=").append(config.propertyNaming().getClass().getName())
                .append(", discriminator=").append(config.discriminator().getClass().getName())
                .append(", discriminatorKey=").append(config.discriminatorKey())
                .append(", propertyDiscovery=").append(config.propertyDiscovery())
                .append(", propertyAnnotationProviders=[");
        String separator = "";
        for (Object provider : config.propertyAnnotationProviders()) {
            key.append(separator).append(provider.getClass().getName());
            separator = ",";
        }
        return key.append("], caseRules=").append(caseRules(Locale.getDefault())).toString();
    }

    /**
     * @param locale the locale
     * @return the language whose rules {@link String#toLowerCase()} and {@link String#toUpperCase()} apply in the locale, or
     *         {@code root} if they're the same as every other language's. Only Turkish, Azerbaijani and Lithuanian differ, so a
     *         model generated under, say, {@code en-US} stays usable under {@code de-DE}.
     */
    static String caseRules(Locale locale) {
        String language = locale.getLanguage();
        return CASE_RULE_LANGUAGES.contains(language) ? language : "root";
    }

    private static String morphiaVersion() {
        try (InputStream in = CritterEntityModel.class.getResourceAsStream("/dev/morphia/morphia-version.properties")) {
            Properties properties = new Properties();
            if (in != null) {
                properties.load(in);
            }
            return properties.getProperty("version", "unknown");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        // only the parameters identify the method: a stand-in's fluent setter returns the stand-in, which the type's loader
        // may not see
        String parametersOnly = MethodTypeDesc.ofDescriptor(descriptor).changeReturnType(ConstantDescs.CD_void).descriptorString();
        Class<?>[] parameters = MethodType.fromMethodDescriptorString(parametersOnly, type.getClassLoader()).parameterArray();
        // the nearest class declaring a match wins, and within it a bridge only if nothing else matches: a class implementing
        // a generic interface may have the erased setter only as a bridge
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Method bridge = null;
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameters)) {
                    if (!method.isBridge()) {
                        return method;
                    }
                    bridge = method;
                }
            }
            if (bridge != null) {
                return bridge;
            }
        }
        throw new IllegalStateException("No method %s%s in %s".formatted(name, descriptor, type.getName()));
    }
}
