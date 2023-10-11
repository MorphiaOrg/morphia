package dev.morphia.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mongodb.lang.Nullable;

import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.QueryFactory;

import org.bson.codecs.configuration.CodecProvider;

import static dev.morphia.mapping.DateStorage.UTC;
import static dev.morphia.mapping.DiscriminatorFunction.simpleName;
import static dev.morphia.mapping.NamingStrategy.camelCase;
import static dev.morphia.mapping.NamingStrategy.identity;
import static dev.morphia.mapping.PropertyDiscovery.FIELDS;
import static java.lang.Boolean.FALSE;

/**
 * @since 2.4
 * @hidden
 */
@SuppressWarnings("removal")
public class ManualMorphiaConfig implements MorphiaConfig {
    Boolean applyCaps;
    Boolean applyDocumentValidations;
    Boolean applyIndexes;
    String database;
    Optional<CodecProvider> codecProvider;
    NamingStrategy collectionNaming;
    DateStorage dateStorage;
    DiscriminatorFunction discriminator;
    String discriminatorKey;
    Boolean enablePolymorphicQueries;
    Boolean ignoreFinals;
    List<String> packages;
    PropertyDiscovery propertyDiscovery;
    NamingStrategy propertyNaming;
    QueryFactory queryFactory;
    Boolean storeEmpties;
    Boolean storeNulls;

    /**
     * @hidden
     */
    public ManualMorphiaConfig() {
    }

    /**
     * @hidden
     */
    protected ManualMorphiaConfig(MorphiaConfig base) {
        applyCaps = base.applyCaps();
        applyDocumentValidations = base.applyDocumentValidations();
        applyIndexes = base.applyIndexes();
        codecProvider = base.codecProvider();
        collectionNaming = base.collectionNaming();
        database = base.database();
        dateStorage = base.dateStorage();
        discriminator = base.discriminator();
        discriminatorKey = base.discriminatorKey();
        enablePolymorphicQueries = base.enablePolymorphicQueries();
        ignoreFinals = base.ignoreFinals();
        packages = new ArrayList<>(base.packages());
        propertyDiscovery = base.propertyDiscovery();
        propertyNaming = base.propertyNaming();
        queryFactory = base.queryFactory();
        storeEmpties = base.storeEmpties();
        storeNulls = base.storeNulls();
    }

    /**
     * @return a new config
     */
    public static ManualMorphiaConfig configure() {
        return new ManualMorphiaConfig();
    }

    /**
     * @param base the config to copy
     * @return the new config
     */
    public static ManualMorphiaConfig configure(MorphiaConfig base) {
        return new ManualMorphiaConfig(base);
    }

    @Override
    public Boolean applyCaps() {
        return orDefault(applyCaps, FALSE);
    }

    public Boolean applyDocumentValidations() {
        return orDefault(applyDocumentValidations, FALSE);
    }

    @Override
    public Boolean applyIndexes() {
        return orDefault(applyIndexes, FALSE);
    }

    @Override
    public Optional<CodecProvider> codecProvider() {
        return orDefault(codecProvider, Optional.empty());
    }

    @Override
    public NamingStrategy collectionNaming() {
        return orDefault(collectionNaming, camelCase());
    }

    @Override
    public String database() {
        return orDefault(database, "morphia");
    }

    @Override
    public DateStorage dateStorage() {
        return orDefault(dateStorage, UTC);
    }

    @Override
    public DiscriminatorFunction discriminator() {
        return orDefault(discriminator, simpleName());
    }

    @Override
    public String discriminatorKey() {
        return orDefault(discriminatorKey, "_t");
    }

    @Override
    public Boolean enablePolymorphicQueries() {
        return orDefault(enablePolymorphicQueries, FALSE);
    }

    @Override
    public Boolean ignoreFinals() {
        return orDefault(ignoreFinals, FALSE);
    }

    @Override
    public List<String> packages() {
        return orDefault(packages, List.of());
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return orDefault(propertyDiscovery, FIELDS);
    }

    @Override
    public NamingStrategy propertyNaming() {
        return orDefault(propertyNaming, identity());
    }

    @Override
    public QueryFactory queryFactory() {
        return orDefault(queryFactory, new DefaultQueryFactory());
    }

    @Override
    public Boolean storeEmpties() {
        return orDefault(storeEmpties, FALSE);
    }

    @Override
    public Boolean storeNulls() {
        return orDefault(storeNulls, FALSE);
    }

    @Override
    public String toString() {
        return ("MorphiaConfig{applyCaps=%s, applyDocumentValidations=%s, applyIndexes=%s, database='%s', codecProvider=%s, " +
                "collectionNaming=%s, dateStorage=%s, discriminator=%s, discriminatorKey='%s', enablePolymorphicQueries=%s, " +
                "ignoreFinals=%s, packages=%s, propertyDiscovery=%s, propertyNaming=%s, queryFactory=%s, " +
                "storeEmpties=%s, storeNulls=%s}").formatted(
                        applyCaps(), applyDocumentValidations(), applyIndexes(), database(), codecProvider(), collectionNaming(),
                        dateStorage(), discriminator(), discriminatorKey(), enablePolymorphicQueries(), ignoreFinals(), packages(),
                        propertyDiscovery(), propertyNaming(), queryFactory(), storeEmpties(), storeNulls());
    }

    protected <T> T orDefault(@Nullable T localValue, T defaultValue) {
        return localValue != null ? localValue : defaultValue;
    }
}
