package dev.morphia.config;

import java.util.List;
import java.util.Optional;

import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.NamingStrategy;

import dev.morphia.query.QueryFactory;
import org.bson.UuidRepresentation;

public class MapperOptionsWrapper implements MorphiaConfig {
    private MapperOptions options;
    private final String database;

    public MapperOptionsWrapper(MapperOptions options, String database) {
        this.options = options;
        this.database = database;
    }

    @Override
    public String database() {
        return database;
    }

    @Override
    public boolean autoImportModels() {
        return false;
    }

    @Override
    public Optional<String> codecProvider() {
        return Optional.empty();
    }

    @Override
    public NamingStrategy collectionNaming() {
        return null;
    }

    @Override
    public Optional<List<String>> conventions() {
        return Optional.empty();
    }

    @Override
    public DateStorage dateStorage() {
        return null;
    }

    @Override
    public DiscriminatorFunction discriminator() {
        return null;
    }

    @Override
    public String discriminatorKey() {
        return null;
    }

    @Override
    public boolean enablePolymorphicQueries() {
        return false;
    }

    @Override
    public boolean ignoreFinals() {
        return false;
    }

    @Override
    public boolean mapSubPackages() {
        return false;
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return null;
    }

    @Override
    public NamingStrategy propertyNaming() {
        return null;
    }

    @Override
    public QueryFactory queryFactory() {
        return null;
    }

    @Override
    public boolean storeEmpties() {
        return false;
    }

    @Override
    public boolean storeNulls() {
        return false;
    }

    @Override
    public UuidRepresentation uuidRepresentation() {
        return null;
    }
}
