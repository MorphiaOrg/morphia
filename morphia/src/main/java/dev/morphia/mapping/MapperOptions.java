package dev.morphia.mapping;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.codec.MorphiaInstanceCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Options to control mapping behavior.
 */
public class MapperOptions {
    public static final MapperOptions DEFAULT = MapperOptions.builder().build();
    private static final Logger LOG = LoggerFactory.getLogger(MapperOptions.class);

    private final boolean ignoreFinals;
    private final boolean storeNulls;
    private final boolean storeEmpties;
    private final boolean cacheClassLookups;
    private final boolean mapSubPackages;
    private final MorphiaInstanceCreator creator;
    private final String discriminatorKey;
    private final DiscriminatorFunction discriminator;
    private final List<MorphiaConvention> conventions;
    private final NamingStrategy collectionNaming;
    private final NamingStrategy fieldNaming;
    private ClassLoader classLoader;

    private MapperOptions(final Builder builder) {
        ignoreFinals = builder.ignoreFinals;
        storeNulls = builder.storeNulls;
        storeEmpties = builder.storeEmpties;
        cacheClassLookups = builder.cacheClassLookups;
        mapSubPackages = builder.mapSubPackages;
        creator = builder.creator;
        classLoader = builder.classLoader;
        discriminatorKey = builder.discriminatorKey;
        discriminator = builder.discriminator;
        conventions = builder.conventions;
        collectionNaming = builder.collectionNaming;
        fieldNaming = builder.fieldNaming;
    }

    /**
     * @return a builder to set mapping options
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a builder to set mapping options
     */
    public static Builder legacy() {
        return new Builder()
                   .discriminatorKey(Mapper.CLASS_NAME_FIELDNAME)
                   .discriminator(DiscriminatorFunction.className)
                   .collectionNaming(NamingStrategy.identity())
                   .fieldNaming(NamingStrategy.identity());
    }

    /**
     * @param original an existing set of options to use as a starting point
     * @return a builder to set mapping options
     */
    public static Builder builder(final MapperOptions original) {
        Builder builder = new Builder();
        builder.ignoreFinals = original.isIgnoreFinals();
        builder.storeNulls = original.isStoreNulls();
        builder.storeEmpties = original.isStoreEmpties();
        builder.cacheClassLookups = original.isCacheClassLookups();
        builder.mapSubPackages = original.isMapSubPackages();
        builder.creator = original.getCreator();
        builder.classLoader = original.getClassLoader();
        return builder;
    }

    /**
     * @return the factory to use when creating new instances
     */
    public MorphiaInstanceCreator getCreator() {
        return creator;
    }

    /**
     * @return true if Morphia should cache name to Class lookups
     */
    public boolean isCacheClassLookups() {
        return cacheClassLookups;
    }

    /**
     * @return true if Morphia should ignore final fields
     */
    public boolean isIgnoreFinals() {
        return ignoreFinals;
    }

    /**
     * @return true if Morphia should store empty values for lists/maps/sets/arrays
     */
    public boolean isStoreEmpties() {
        return storeEmpties;
    }

    /**
     * @return true if Morphia should store null values
     */
    public boolean isStoreNulls() {
        return storeNulls;
    }

    /**
     * @return true if Morphia should map classes from the sub-packages as well
     */
    public boolean isMapSubPackages() {
        return mapSubPackages;
    }

    /**
     * Returns the classloader used, in theory, when loading the entity types.
     *
     * @return the classloader
     * @morphia.internal
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    /**
     * @return the naming strategy for collections unless explicitly set via @Entity
     * @see Entity
     */
    public NamingStrategy getCollectionNaming() {
        return collectionNaming;
    }

    /**
     * @return the discriminator field name
     */
    public String getDiscriminatorKey() {
        return discriminatorKey;
    }

    /**
     * @return the function to determine discriminator value
     */
    public DiscriminatorFunction getDiscriminator() {
        return discriminator;
    }

    /**
     * @return the configured Conventions
     */
    public List<MorphiaConvention> getConventions() {
        return Collections.unmodifiableList(conventions);
    }

    /**
     * @return the naming strategy for fields unless explicitly set via @Property
     * @see Property
     */
    public NamingStrategy getFieldNaming() {
        return fieldNaming;
    }

    /**
     * A builder class for setting mapping options
     */
    @SuppressWarnings("unused")
    public static final class Builder {

        private boolean ignoreFinals;
        private boolean storeNulls;
        private boolean storeEmpties;
        private boolean cacheClassLookups;
        private boolean mapSubPackages;
        private MorphiaInstanceCreator creator;
        private ClassLoader classLoader;
        private String discriminatorKey = "_t";
        private DiscriminatorFunction discriminator = DiscriminatorFunction.simpleName;
        private List<MorphiaConvention> conventions = new ArrayList<>(List.of(new MorphiaDefaultsConvention()));
        private NamingStrategy collectionNaming = NamingStrategy.identity();
        private NamingStrategy fieldNaming = NamingStrategy.identity();

        private Builder() {
        }

        /**
         * @param ignoreFinals if true final fields are ignored
         * @return this
         */
        public Builder ignoreFinals(final boolean ignoreFinals) {
            this.ignoreFinals = ignoreFinals;
            return this;
        }

        /**
         * @param storeNulls if true null values are stored in the database
         * @return this
         */
        public Builder storeNulls(final boolean storeNulls) {
            this.storeNulls = storeNulls;
            return this;
        }

        /**
         * @param storeEmpties if true empty maps and collection types are stored in the database
         * @return this
         */
        public Builder storeEmpties(final boolean storeEmpties) {
            this.storeEmpties = storeEmpties;
            return this;
        }

        /**
         * @param useLowerCaseCollectionNames if true, generated collections names are lower cased
         * @return this
         * @deprecated use {@link #collectionNaming(NamingStrategy)} instead
         */
        @Deprecated(since = "2.0", forRemoval = true)
        public Builder useLowerCaseCollectionNames(final boolean useLowerCaseCollectionNames) {
            if (useLowerCaseCollectionNames) {
                collectionNaming(NamingStrategy.lowerCase());
            }
            return this;
        }

        /**
         * @param cacheClassLookups if true class lookups are cached
         * @return this
         */
        public Builder cacheClassLookups(final boolean cacheClassLookups) {
            this.cacheClassLookups = cacheClassLookups;
            return this;
        }

        /**
         * @param mapSubPackages if true subpackages are mapped when given a particular package
         * @return this
         */
        public Builder mapSubPackages(final boolean mapSubPackages) {
            this.mapSubPackages = mapSubPackages;
            return this;
        }

        /**
         * @param disableEmbeddedIndexes if true scanning @Embedded fields for indexing is disabled
         * @return this
         */
        public Builder disableEmbeddedIndexes(final boolean disableEmbeddedIndexes) {
            LOG.warn("this option is no longer used");
            return this;
        }

        /**
         * @param creator the object factory to use when creating instances
         * @return this
         */
        public Builder objectFactory(final MorphiaInstanceCreator creator) {
            this.creator = creator;
            return this;
        }

        /**
         * @param classLoader the ClassLoader to use
         * @return this
         */
        public Builder classLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Defines the discriminator key name
         *
         * @param key the key to use, e.g., "_t".  the default/legacy value is "className"
         * @return this
         */
        public Builder discriminatorKey(final String key) {
            this.discriminatorKey = key;
            return this;
        }

        /**
         * Sets the discriminator function to use
         *
         * @param function the function to use
         * @return this
         */
        public Builder discriminator(final DiscriminatorFunction function) {
            this.discriminator = function;
            return this;
        }

        /**
         * Sets the naming strategy to use for collection names
         *
         * @param strategy the strategy to use
         * @return this
         */
        public Builder collectionNaming(final NamingStrategy strategy) {
            this.collectionNaming = strategy;
            return this;
        }

        /**
         * Sets the naming strategy to use for fields unless expliclity set via @Property
         *
         * @param strategy the strategy to use
         * @return this
         * @see Property
         */
        public Builder fieldNaming(final NamingStrategy strategy) {
            this.collectionNaming = strategy;
            return this;
        }

        /**
         * Adds a custom convention to the list to be applied to all new MorphiaModels.
         *
         * @param convention the new convention
         * @return this
         * @since 2.0
         */
        public Builder addConvention(final MorphiaConvention convention) {
            conventions.add(convention);

            return this;
        }

        /**
         * @return the new options instance
         */
        public MapperOptions build() {
            return new MapperOptions(this);
        }
    }
}
