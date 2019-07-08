package dev.morphia.mapping;


import dev.morphia.ObjectFactory;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.cache.DefaultEntityCacheFactory;
import dev.morphia.mapping.cache.EntityCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Options to control mapping behavior.
 */
public class MapperOptions {
    private static final Logger LOG = LoggerFactory.getLogger(MapperOptions.class);
    private final List<Class<? extends Annotation>> propertyHandlers;
    private final boolean ignoreFinals; //ignore final fields.
    private final boolean storeNulls;
    private final boolean storeEmpties;
    private final boolean useLowerCaseCollectionNames;
    private final boolean cacheClassLookups;
    private final boolean mapSubPackages;
    private final EntityCacheFactory cacheFactory;
    private final DateStorage dateStorage ;
    private final ObjectFactory objectFactory;

    private MapperOptions(final Builder builder) {
        ignoreFinals = builder.ignoreFinals;
        storeNulls = builder.storeNulls;
        storeEmpties = builder.storeEmpties;
        useLowerCaseCollectionNames = builder.useLowerCaseCollectionNames;
        cacheClassLookups = builder.cacheClassLookups;
        mapSubPackages = builder.mapSubPackages;
        objectFactory = builder.objectFactory;
        cacheFactory = builder.cacheFactory;
        dateStorage = builder.dateStorage;
        propertyHandlers = builder.propertyHandlers;
    }

    /**
     * @return the factory to create an EntityCache
     */
    public EntityCacheFactory getCacheFactory() {
        return cacheFactory;
    }

    /**
     * @return the factory to use when creating new instances
     */
    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public List<Class<? extends Annotation>> getPropertyHandlers() {
        return propertyHandlers;
    }

    /**
     * @return true if Morphia should cache name -> Class lookups
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
     * @return true if Morphia should use lower case values when calculating collection names
     */
    public boolean isUseLowerCaseCollectionNames() {
        return useLowerCaseCollectionNames;
    }

    /**
     * @return true if Morphia should map classes from the sub-packages as well
     */
    public boolean isMapSubPackages() {
        return mapSubPackages;
    }

    /**
     * @return the discriminator field name
     */
    public String getDiscriminatorField() {
        return Mapper.CLASS_NAME_FIELDNAME;
    }

    /**
     * @return the format to use for Java 8 date/time storage
     */
    public DateStorage getDateStorage() {
        return dateStorage;
    }

    /**
     * @return a builder to set mapping options
     */
    public static Builder builder() {
        return new Builder();
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
        builder.useLowerCaseCollectionNames = original.isUseLowerCaseCollectionNames();
        builder.cacheClassLookups = original.isCacheClassLookups();
        builder.mapSubPackages = original.isMapSubPackages();
        builder.objectFactory = original.getObjectFactory();
        builder.cacheFactory = original.getCacheFactory();
        return builder;
    }

    /**
     * A builder class for setting mapping options
     */
    @SuppressWarnings("unused")
    public static final class Builder {

        private boolean ignoreFinals;
        private boolean storeNulls;
        private boolean storeEmpties;
        private boolean useLowerCaseCollectionNames;
        private boolean cacheClassLookups;
        private boolean mapSubPackages;
        private DateStorage dateStorage = DateStorage.UTC;
        private ObjectFactory objectFactory;
        private EntityCacheFactory cacheFactory = new DefaultEntityCacheFactory();
        private final List<Class<? extends Annotation>> propertyHandlers = new ArrayList<>();

        private Builder() {
            addPropertyHandler(Reference.class);
        }

        private Builder addPropertyHandler(final Class<? extends Annotation> annotation) {
            propertyHandlers.add(annotation);
            return this;
        }

        /**
         * this method does nothing
         *
         * @param actLikeSerializer unused
         * @return this
         * @deprecated
         */
        @Deprecated
        public Builder actLikeSerializer(final boolean actLikeSerializer) {
            return this;
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
         */
        public Builder useLowerCaseCollectionNames(final boolean useLowerCaseCollectionNames) {
            this.useLowerCaseCollectionNames = useLowerCaseCollectionNames;
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
         * @param objectFactory the object factory to use when creating instances
         * @return this
         */
        public Builder objectFactory(final ObjectFactory objectFactory) {
            this.objectFactory = objectFactory;
            return this;
        }

        /**
         * @param cacheFactory the cache factory to use
         * @return this
         */
        public Builder cacheFactory(final EntityCacheFactory cacheFactory) {
            this.cacheFactory = cacheFactory;
            return this;
        }

        /**
         * @param dateStorage the storage format to use for dates
         * @return this
         * @deprecated This will be removed in 2.0.  It is intended to bridge the gap when correcting the storage of data/time values in the
         * database.  {@link DateStorage#UTC} should be used and will be the default in 2.0.  In 1.5 it is
         * {@link DateStorage#SYSTEM_DEFAULT} for backwards compatibility.
         */
        @Deprecated
        public Builder dateForm(final DateStorage dateStorage) {
            this.dateStorage = dateStorage;
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
