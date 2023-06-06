package dev.morphia.config;

import java.util.List;
import java.util.Optional;

import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.NamingStrategy;

import dev.morphia.query.QueryFactory;
import org.bson.UuidRepresentation;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "morphia")
public interface MorphiaConfig {
    String database();

    @WithDefault("true")
    boolean autoImportModels();

    //        cacheClassLookups
    //    classLoader
    Optional<String> codecProvider();

    @WithDefault("camelCase")
    @WithConverter(NamingStrategyConverter.class)
    NamingStrategy collectionNaming();

    Optional<List<String>> conventions();

    @WithDefault("UTC")
    DateStorage dateStorage();

    @WithDefault("simpleName")
    @WithConverter(DiscriminatorFunctionConverter.class)
    DiscriminatorFunction discriminator();

    @WithDefault("_t")
    String discriminatorKey();

    @WithDefault("false")
    boolean enablePolymorphicQueries();

    @WithDefault("false")
    boolean ignoreFinals();

    @WithDefault("false")
    boolean mapSubPackages();

    @WithDefault("FIELDS")
    PropertyDiscovery propertyDiscovery();

    @WithDefault("identity")
    @WithConverter(NamingStrategyConverter.class)
    NamingStrategy propertyNaming();

    @WithConverter(QueryFactoryConverter.class)
    @WithDefault("dev.morphia.query.DefaultQueryFactory")
    QueryFactory queryFactory();

    @WithDefault("false")
    boolean storeEmpties();

    @WithDefault("false")
    boolean storeNulls();

    @WithDefault("STANDARD")
    UuidRepresentation uuidRepresentation();
}
