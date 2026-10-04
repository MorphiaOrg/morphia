package dev.morphia.mapping;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.critter.CritterClassLoader;
import dev.morphia.critter.parser.generator.CritterGenerator;
import dev.morphia.critter.parser.generator.EntityModelGenerator;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.critter.Critter.critterPackage;

/**
 * Hybrid mapper using three-tier entity model discovery:
 * <ol>
 * <li>Pre-generated models from the classpath (critter-maven AOT)</li>
 * <li>Runtime bytecode+VarHandle generation</li>
 * <li>Reflection-based fallback</li>
 * </ol>
 *
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public class CritterMapper extends AbstractMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CritterMapper.class);

    private final RuntimeModels runtimeModels;
    private final CritterGenerator generator;
    private final Set<String> fallbackTypes;

    /**
     * Creates a CritterMapper with the given config.
     *
     * @param config the config to use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public CritterMapper(MorphiaConfig config) {
        this(config, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a CritterMapper with the given config.
     *
     * @param config      the config to use
     * @param classLoader the class loader to use for loading generated classes
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public CritterMapper(MorphiaConfig config, ClassLoader classLoader) {
        super(config, classLoader);
        this.runtimeModels = RuntimeModels.forConfig(config, classLoader);
        this.generator = new CritterGenerator(this);
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
     *
     * @param other the original to copy
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public CritterMapper(CritterMapper other) {
        this(other, other.config);
    }

    /**
     * Copy constructor that reuses another mapper's entity graph under a different config
     * (e.g. a different database name) — see {@link #CritterMapper(CritterMapper)} for why
     * this shares immutable {@code CritterEntityModel} references and creates a new
     * {@code DiscriminatorLookup} rather than delegating to {@code AbstractMapper}'s copy
     * constructor.
     *
     * @param other  the original to clone the entity graph from
     * @param config the config the new mapper should report/operate under
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public CritterMapper(CritterMapper other, MorphiaConfig config) {
        super(config, other.classLoader);
        this.runtimeModels = other.runtimeModels;
        this.generator = new CritterGenerator(this);
        this.fallbackTypes = other.fallbackTypes;
        this.listeners.addAll(other.listeners);
        // Create independent copies of all entity models so that each mapper instance
        // maintains its own model state (e.g. listeners, version tracking).
        other.mappedEntities.values().forEach(model -> {
            if (model instanceof CritterEntityModel) {
                try {
                    java.lang.reflect.Constructor<?> ctor = model.getClass().getConstructor(Mapper.class);
                    register((EntityModel) ctor.newInstance(this), false);
                } catch (Exception e) {
                    LOG.warn("Failed to clone CritterEntityModel for {}; sharing reference: {}",
                            model.getType().getName(), e.getMessage());
                    register(model, false);
                }
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
        } catch (Throwable e) {
            LOG.warn("Failed to load pre-generated model for {}: {}", type.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Tier 2: Generate an entity model at runtime using VarHandle accessors.
     * On failure, logs once per type and returns null so the caller falls through to reflection.
     */
    @Nullable
    private EntityModel tryRuntimeGeneration(Class<?> type) {
        try {
            Class<?> modelClass = runtimeModels.modelClass(type, generator);
            Constructor<?> ctor = modelClass.getConstructor(Mapper.class);
            return (EntityModel) ctor.newInstance(this);
        } catch (Exception e) {
            if (fallbackTypes.add(type.getName())) {
                LOG.warn("Runtime bytecode generation failed for {}; falling back to reflection: {}",
                        type.getName(), e.getMessage(), e);
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

    /**
     * The runtime-generated model classes for one parent class loader and mapping configuration. The
     * {@link CritterClassLoader} is created the first time a model has to be generated at runtime; when every model is
     * pre-generated, it is never needed.
     * <p>
     * Copies of a mapper share the same instance. So do separate mappers with the same parent loader and an equivalent
     * configuration (see {@link #generationKey(MorphiaConfig)}), so each entity is generated once rather than once per
     * mapper. Instances are cached weakly: once no mapper refers to one, its loader and classes can be collected.
     */
    private static final class RuntimeModels {
        private static final Map<ClassLoader, Map<List<Object>, WeakReference<RuntimeModels>>> SHARED = new WeakHashMap<>();

        private final ClassLoader parent;
        private final Map<Class<?>, Class<?>> modelClasses = new HashMap<>();
        @Nullable
        private CritterClassLoader loader;

        private RuntimeModels(ClassLoader parent) {
            this.parent = parent;
            this.loader = parent instanceof CritterClassLoader ccl ? ccl : null;
        }

        /**
         * @return the shared instance for this loader and configuration, or a new unshared one if the configuration
         *         can't be compared safely or {@code parent} is itself a {@link CritterClassLoader}
         */
        private static RuntimeModels forConfig(MorphiaConfig config, ClassLoader parent) {
            List<Object> key = parent instanceof CritterClassLoader ? null : generationKey(config);
            if (key == null) {
                return new RuntimeModels(parent);
            }
            synchronized (SHARED) {
                Map<List<Object>, WeakReference<RuntimeModels>> byConfig = SHARED.computeIfAbsent(parent, p -> new HashMap<>());
                byConfig.values().removeIf(ref -> ref.get() == null);
                WeakReference<RuntimeModels> ref = byConfig.get(key);
                RuntimeModels models = ref != null ? ref.get() : null;
                if (models == null) {
                    models = new RuntimeModels(parent);
                    byConfig.put(key, new WeakReference<>(models));
                }
                return models;
            }
        }

        /**
         * Returns the model class for {@code type}, generating it the first time any mapper sharing this instance asks.
         */
        private synchronized Class<?> modelClass(Class<?> type, CritterGenerator generator) throws ClassNotFoundException {
            Class<?> modelClass = modelClasses.get(type);
            if (modelClass == null) {
                if (loader == null) {
                    loader = new CritterClassLoader(parent);
                }
                EntityModelGenerator entityModel = generator.generate(type, loader, true);
                modelClass = loader.loadClass(entityModel.getGeneratedType());
                modelClasses.put(type, modelClass);
            }
            return modelClass;
        }
    }

    /**
     * The configuration settings that runtime generation bakes into a model's bytecode. Two configurations with equal
     * keys produce identical models, so their mappers can share generated classes.
     *
     * @return the key, or {@code null} if a strategy isn't one of Morphia's own, in which case two instances of the same
     *         class could still behave differently and the models aren't shared
     */
    @Nullable
    private static List<Object> generationKey(MorphiaConfig config) {
        List<Object> strategies = new ArrayList<>();
        strategies.add(config.collectionNaming());
        strategies.add(config.propertyNaming());
        strategies.add(config.discriminator());
        strategies.addAll(config.propertyAnnotationProviders());

        List<Object> key = new ArrayList<>();
        for (Object strategy : strategies) {
            Class<?> strategyClass = strategy.getClass();
            if (!strategyClass.getName().startsWith("dev.morphia.") || strategyClass.isAnonymousClass()
                    || strategyClass.isSynthetic()) {
                return null;
            }
            key.add(strategyClass.getName());
        }
        key.add(config.discriminatorKey());
        key.add(config.propertyDiscovery());
        return key;
    }
}
