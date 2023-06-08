package dev.morphia.config;

import java.util.Optional;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.query.QueryFactory;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public class MapperOptionsWrapper implements MorphiaConfig {
    private static final Logger LOG = LoggerFactory.getLogger(MapperOptions.class);

    private final MapperOptions options;
    private final String database;

    public MapperOptionsWrapper(MapperOptions options, String database) {
        this.options = options;
        this.database = database;
        logConfigMessage();
    }

    private void logConfigMessage() {
        LOG.info("Morphia 3.0 will be moving to a configuration file based setup.  As such MapperOptions will be removed in the next " +
                "major release.  To remove this message, create the file 'META-INF/morphia-config.properties' in your resources folder " +
                "using the following text.  Entries with default values may be omitted but are included here for completeness.\n" +
                new MorphiaConfigDocumenter(this));
    }

    @Override
    public String database() {
        return database;
    }

    @Override
    public Optional<CodecProvider> codecProvider() {
        return Optional.ofNullable(options.codecProvider());
    }

    @Override
    public NamingStrategy collectionNaming() {
        return options.getCollectionNaming();
    }

    @Override
    public DateStorage dateStorage() {
        return options.getDateStorage();
    }

    @Override
    public DiscriminatorFunction discriminator() {
        return options.getDiscriminator();
    }

    @Override
    public String discriminatorKey() {
        return options.getDiscriminatorKey();
    }

    @Override
    public boolean enablePolymorphicQueries() {
        return options.isEnablePolymorphicQueries();
    }

    @Override
    public boolean ignoreFinals() {
        return options.isIgnoreFinals();
    }

    @Override
    public boolean mapSubpackages() {
        return options.isMapSubPackages();
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return options.propertyDiscovery();
    }

    @Override
    public NamingStrategy propertyNaming() {
        return options.getPropertyNaming();
    }

    @Override
    public QueryFactory queryFactory() {
        return options.getQueryFactory();
    }

    @Override
    public boolean storeEmpties() {
        return options.isStoreEmpties();
    }

    @Override
    public boolean storeNulls() {
        return options.isStoreNulls();
    }

    @Override
    @SuppressWarnings("removal")
    public UuidRepresentation uuidRepresentation() {
        return options.getUuidRepresentation();
    }
}
