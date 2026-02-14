package dev.morphia.config;

import java.util.List;
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

import static java.util.Collections.emptyList;

/**
 * @hidden
 * @morphia.internal
 * @since 2.4
 */
@SuppressWarnings("removal")
@MorphiaInternal
public class MapperOptionsWrapper implements MorphiaConfig {

    private final MapperOptions options;
    private final String database;

    public MapperOptionsWrapper(MapperOptions options, String database) {
        this.options = options;
        this.database = database;
    }

    @Override
    public Boolean applyCaps() {
        return false;
    }

    @Override
    public Boolean applyDocumentValidations() {
        return false;
    }

    @Override
    public Boolean applyIndexes() {
        return false;
    }

    @Override
    public Boolean autoImportModels() {
        return options.autoImportModels();
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
    public Boolean enablePolymorphicQueries() {
        return options.isEnablePolymorphicQueries();
    }

    @Override
    public Boolean ignoreFinals() {
        return options.isIgnoreFinals();
    }

    @Override
    public List<String> packages() {
        return emptyList();
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
    public Boolean storeEmpties() {
        return options.isStoreEmpties();
    }

    @Override
    public Boolean storeNulls() {
        return options.isStoreNulls();
    }

    @Override
    @SuppressWarnings("removal")
    public UuidRepresentation uuidRepresentation() {
        return options.getUuidRepresentation();
    }
}
