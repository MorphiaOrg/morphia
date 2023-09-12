package dev.morphia.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.LegacyQueryFactory;
import dev.morphia.query.QueryFactory;
import dev.morphia.sofia.Sofia;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;

import static dev.morphia.mapping.DateStorage.UTC;
import static dev.morphia.mapping.DiscriminatorFunction.className;
import static dev.morphia.mapping.DiscriminatorFunction.simpleName;
import static dev.morphia.mapping.MapperOptions.PropertyDiscovery.FIELDS;
import static dev.morphia.mapping.NamingStrategy.camelCase;
import static dev.morphia.mapping.NamingStrategy.identity;
import static java.lang.Boolean.FALSE;

/**
 * @since 2.4
 */
@SuppressWarnings("removal")
public class MorphiaConfigBuilder implements MorphiaConfig {
    private Boolean applyCaps;
    private Boolean applyDocumentValidations;
    private Boolean applyIndexes;
    private String database;
    private Optional<CodecProvider> codecProvider;
    private NamingStrategy collectionNaming;
    private DateStorage dateStorage;
    private DiscriminatorFunction discriminator;
    private String discriminatorKey;
    private Boolean enablePolymorphicQueries;
    private Boolean ignoreFinals;
    private List<String> packages;
    private PropertyDiscovery propertyDiscovery;
    private NamingStrategy propertyNaming;
    private QueryFactory queryFactory;
    private Boolean storeEmpties;
    private Boolean storeNulls;
    private UuidRepresentation uuidRepresentation;

    /**
     * @hidden
     */
    public MorphiaConfigBuilder() {
    }

    /**
     * @hidden
     */
    public MorphiaConfigBuilder(MorphiaConfig base) {
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
        uuidRepresentation = base.uuidRepresentation();
    }

    public static MorphiaConfigBuilder configure() {
        return new MorphiaConfigBuilder();
    }

    public static MorphiaConfigBuilder configure(MorphiaConfig base) {
        return new MorphiaConfigBuilder(base);
    }

    @Override
    public Boolean applyCaps() {
        return orDefault(applyCaps, FALSE);
    }

    public MorphiaConfigBuilder applyCaps(Boolean applyCaps) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.applyCaps = applyCaps;
        return newConfig;
    }

    public Boolean applyDocumentValidations() {
        return orDefault(applyDocumentValidations, FALSE);
    }

    public MorphiaConfigBuilder applyDocumentValidations(Boolean applyDocumentValidations) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.applyDocumentValidations = applyDocumentValidations;
        return newConfig;
    }

    @Override
    public Boolean applyIndexes() {
        return orDefault(applyIndexes, FALSE);
    }

    public MorphiaConfigBuilder applyIndexes(Boolean applyIndexes) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.applyIndexes = applyIndexes;
        return newConfig;
    }

    @Override
    public Optional<CodecProvider> codecProvider() {
        return orDefault(codecProvider, Optional.empty());
    }

    public MorphiaConfigBuilder codecProvider(CodecProvider codecProvider) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.codecProvider = Optional.of(codecProvider);
        return newConfig;
    }

    @Override
    public NamingStrategy collectionNaming() {
        return orDefault(collectionNaming, camelCase());
    }

    public MorphiaConfigBuilder collectionNaming(NamingStrategy collectionNaming) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.collectionNaming = collectionNaming;
        return newConfig;
    }

    @Override
    public String database() {
        if (database != null) {
            return database;
        }
        throw new IllegalStateException(Sofia.databaseRequired());
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public MorphiaConfigBuilder database(String database) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.database = database;
        return newConfig;
    }

    @Override
    public DateStorage dateStorage() {
        return orDefault(dateStorage, UTC);
    }

    public MorphiaConfigBuilder dateStorage(DateStorage dateStorage) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.dateStorage = dateStorage;
        return newConfig;
    }

    @Override
    public DiscriminatorFunction discriminator() {
        return orDefault(discriminator, simpleName());
    }

    public MorphiaConfigBuilder discriminator(DiscriminatorFunction discriminator) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.discriminator = discriminator;
        return newConfig;
    }

    @Override
    public String discriminatorKey() {
        return orDefault(discriminatorKey, "_t");
    }

    public MorphiaConfigBuilder discriminatorKey(String discriminatorKey) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.discriminatorKey = discriminatorKey;
        return newConfig;
    }

    @Override
    public Boolean enablePolymorphicQueries() {
        return orDefault(enablePolymorphicQueries, FALSE);
    }

    public MorphiaConfigBuilder enablePolymorphicQueries(Boolean enablePolymorphicQueries) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.enablePolymorphicQueries = enablePolymorphicQueries;
        return newConfig;
    }

    @Override
    public Boolean ignoreFinals() {
        return orDefault(ignoreFinals, FALSE);
    }

    public MorphiaConfigBuilder ignoreFinals(Boolean ignoreFinals) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.ignoreFinals = ignoreFinals;
        return newConfig;
    }

    public MorphiaConfig legacy() {
        MorphiaConfigBuilder newConfig = new MorphiaConfigBuilder(this);
        newConfig.dateStorage = DateStorage.SYSTEM_DEFAULT;
        newConfig.discriminatorKey = "className";
        newConfig.discriminator = className();
        newConfig.collectionNaming = identity();
        newConfig.propertyNaming = identity();
        newConfig.queryFactory = new LegacyQueryFactory();

        return newConfig;

    }

    @Override
    public List<String> packages() {
        return packages;
    }

    public MorphiaConfigBuilder packages(List<String> packages) {
        var newConfig = new MorphiaConfigBuilder(this);

        if (!packages.isEmpty()) {
            newConfig.packages = new ArrayList<>(packages);
        }
        return newConfig;
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return orDefault(propertyDiscovery, FIELDS);
    }

    public MorphiaConfigBuilder propertyDiscovery(PropertyDiscovery propertyDiscovery) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.propertyDiscovery = propertyDiscovery;
        return newConfig;
    }

    @Override
    public NamingStrategy propertyNaming() {
        return orDefault(propertyNaming, identity());
    }

    public MorphiaConfigBuilder propertyNaming(NamingStrategy propertyNaming) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.propertyNaming = propertyNaming;
        return newConfig;
    }

    @Override
    public QueryFactory queryFactory() {
        return orDefault(queryFactory, new DefaultQueryFactory());
    }

    public MorphiaConfigBuilder queryFactory(QueryFactory queryFactory) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.queryFactory = queryFactory;
        return newConfig;
    }

    @Override
    public Boolean storeEmpties() {
        return orDefault(storeEmpties, FALSE);
    }

    public MorphiaConfigBuilder storeEmpties(Boolean storeEmpties) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.storeEmpties = storeEmpties;
        return newConfig;
    }

    @Override
    public Boolean storeNulls() {
        return orDefault(storeNulls, FALSE);
    }

    public MorphiaConfigBuilder storeNulls(Boolean storeNulls) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.storeNulls = storeNulls;
        return newConfig;
    }

    @Override
    public UuidRepresentation uuidRepresentation() {
        return orDefault(uuidRepresentation, UuidRepresentation.STANDARD);
    }

    public MorphiaConfigBuilder uuidRepresentation(UuidRepresentation uuidRepresentation) {
        var newConfig = new MorphiaConfigBuilder(this);

        newConfig.uuidRepresentation = uuidRepresentation;
        return newConfig;
    }

    @Override
    public String toString() {
        return ("MorphiaConfigBuilder{applyCaps=%s, applyDocumentValidations=%s, applyIndexes=%s, database='%s', codecProvider=%s, " +
                "collectionNaming=%s, dateStorage=%s, discriminator=%s, discriminatorKey='%s', enablePolymorphicQueries=%s, " +
                "ignoreFinals=%s, packages=%s, propertyDiscovery=%s, propertyNaming=%s, queryFactory=%s, " +
                "storeEmpties=%s, storeNulls=%s, uuidRepresentation=%s}").formatted(
                        applyCaps(), applyDocumentValidations(), applyIndexes(), database(), codecProvider(), collectionNaming(),
                        dateStorage(),
                        discriminator(), discriminatorKey(), enablePolymorphicQueries(), ignoreFinals(), packages(),
                        propertyDiscovery(), propertyNaming(), queryFactory(), storeEmpties(), storeNulls(), uuidRepresentation());
    }

    private <T> T orDefault(T localValue, T defaultValue) {
        return localValue != null ? localValue : defaultValue;
    }
}
