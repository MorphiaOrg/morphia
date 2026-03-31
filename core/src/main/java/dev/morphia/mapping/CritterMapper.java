package dev.morphia.mapping;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.gizmo.CritterGizmoGenerator;
import dev.morphia.critter.parser.gizmo.GizmoEntityModelGenerator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.critter.Critter.critterPackage;

/**
 * Hybrid mapper using three-tier entity model discovery:
 * <ol>
 * <li>Pre-generated models from the classpath (critter-maven AOT)</li>
 * <li>Runtime Gizmo+VarHandle generation</li>
 * <li>Reflection-based fallback</li>
 * </ol>
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class CritterMapper extends AbstractMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CritterMapper.class);

    private final CritterClassLoader critterClassLoader;
    private final CritterGizmoGenerator gizmoGenerator;
    private final Set<String> fallbackTypes;

    /**
     * Creates a CritterMapper with the given config.
     *
     * @param config the config to use
     */
    @MorphiaInternal
    public CritterMapper(MorphiaConfig config) {
        super(config);
        this.critterClassLoader = new CritterClassLoader(contextClassLoader);
        this.gizmoGenerator = new CritterGizmoGenerator(this);
        this.fallbackTypes = ConcurrentHashMap.newKeySet();
    }

    /**
     * Copy constructor — shares immutable CritterEntityModel references,
     * creates a new DiscriminatorLookup for session isolation.
     * <p>
     * Note: calls {@code super(config, classLoader)} rather than {@code super(other)} because
     * {@code AbstractMapper}'s copy constructor calls {@code new EntityModel(original)} for every
     * entity, which is incorrect for {@code CritterEntityModel}. As a side effect, this creates a
     * fresh {@code Conversions} instance instead of sharing {@code other.conversions}. Any custom
     * converters registered on the original mapper after construction will not be visible in the
     * copy. If sharing custom converters becomes necessary, consider adding a package-private
     * accessor on {@code AbstractMapper} for its {@code conversions} field.
     * </p>
     */
    private CritterMapper(CritterMapper other) {
        super(other.config, other.contextClassLoader);
        this.critterClassLoader = other.critterClassLoader;
        this.gizmoGenerator = other.gizmoGenerator;
        this.fallbackTypes = other.fallbackTypes;
        this.listeners.addAll(other.listeners);
        // Share CritterEntityModel refs; clone reflective fallback models
        other.mappedEntities.values().forEach(model -> {
            if (model instanceof CritterEntityModel) {
                register(model, false);
            } else {
                register(new EntityModel(model), false);
            }
        });
    }

    @Override
    public Mapper copy() {
        return new CritterMapper(this);
    }

    // Synchronized to prevent concurrent threads from both passing the initial
    // mappedEntities.get() check and racing to register the same type, which
    // would cause a duplicate discriminator value error in DiscriminatorLookup.
    @Override
    @Nullable
    public synchronized EntityModel mapEntity(@Nullable Class type) {
        if (!isMappable(type)) {
            return null;
        }

        EntityModel model = mappedEntities.get(type.getName());
        if (model != null) {
            return model;
        }

        model = tryLoadPregenerated(type);
        if (model == null) {
            model = tryRuntimeGeneration(type);
        }
        if (model == null) {
            model = fallbackToReflection(type);
        }

        return model != null ? register(model) : null;
    }

    /**
     * Tier 1: Attempt to load a pre-generated model class placed on the classpath
     * by critter-maven. The naming convention is:
     * {@code {package}.__morphia.{simpleNameLowercase}.{SimpleName}EntityModel}.
     */
    @Nullable
    private EntityModel tryLoadPregenerated(Class<?> type) {
        String modelClassName = critterPackage(type) + "." + type.getSimpleName() + "EntityModel";
        try {
            Class<?> modelClass = Class.forName(modelClassName, true, type.getClassLoader());
            Constructor<?> ctor = modelClass.getConstructor(Mapper.class);
            return (EntityModel) ctor.newInstance(this);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            LOG.error("Failed to load pre-generated model for {}: {}", type.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Tier 2: Generate an entity model at runtime using Gizmo + VarHandle accessors.
     * On failure, logs once per type and returns null so the caller falls through to reflection.
     */
    @Nullable
    private EntityModel tryRuntimeGeneration(Class<?> type) {
        try {
            GizmoEntityModelGenerator generator = gizmoGenerator.generate(type, critterClassLoader, true);
            Class<?> modelClass = critterClassLoader.loadClass(generator.getGeneratedType());
            Constructor<?> ctor = modelClass.getConstructor(Mapper.class);
            return (EntityModel) ctor.newInstance(this);
        } catch (Exception e) {
            if (fallbackTypes.add(type.getName())) {
                LOG.error("Runtime bytecode generation failed for {}; falling back to reflection: {}",
                        type.getName(), e.getMessage());
            }
            return null;
        }
    }

    /**
     * Tier 3: Fall back to the standard reflection-based EntityModel.
     */
    private EntityModel fallbackToReflection(Class<?> type) {
        return new EntityModel(this, type);
    }
}
