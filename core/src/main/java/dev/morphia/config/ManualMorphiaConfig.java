package dev.morphia.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperType;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.mapping.PropertyDiscovery;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.QueryFactory;

import org.bson.codecs.configuration.CodecProvider;

import static dev.morphia.config.MorphiaConfigHelper.dumpConfigurationFile;
import static dev.morphia.mapping.DateStorage.UTC;
import static dev.morphia.mapping.DiscriminatorFunction.className;
import static dev.morphia.mapping.DiscriminatorFunction.simpleName;
import static dev.morphia.mapping.NamingStrategy.camelCase;
import static dev.morphia.mapping.NamingStrategy.identity;
import static dev.morphia.mapping.PropertyDiscovery.FIELDS;
import static java.lang.Boolean.FALSE;

/**
 * @since 2.4
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("removal")
public class ManualMorphiaConfig implements MorphiaConfig {
    private Boolean applyCaps;
    private Boolean applyDocumentValidations;
    private Boolean applyIndexes;

    private Optional<CodecProvider> codecProvider;

    private NamingStrategy collectionNaming;

    private String database;

    private DateStorage dateStorage;
    private DiscriminatorFunction discriminator;
    private String discriminatorKey;
    private Boolean enablePolymorphicQueries;
    private Boolean ignoreFinals;
    private MapperType mapper;
    private List<String> packages;
    private PropertyDiscovery propertyDiscovery;
    private List<PropertyAnnotationProvider<?>> propertyAnnotationProviders;
    private NamingStrategy propertyNaming;
    private QueryFactory queryFactory;
    private Boolean storeEmpties;
    private Boolean storeNulls;

    /**
     * @hidden
     */
    protected ManualMorphiaConfig() {
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
        mapper = base.mapper();
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
    public String toString() {
        return ("MorphiaConfig{applyCaps=%s, applyDocumentValidations=%s, applyIndexes=%s, database='%s', codecProvider=%s, " +
                "collectionNaming=%s, dateStorage=%s, discriminator=%s, discriminatorKey='%s', enablePolymorphicQueries=%s, " +
                "ignoreFinals=%s, mapper=%s, packages=%s, propertyDiscovery=%s, propertyNaming=%s, queryFactory=%s, " +
                "storeEmpties=%s, storeNulls=%s}").formatted(
                        applyCaps(), applyDocumentValidations(), applyIndexes(), database(), codecProvider(), collectionNaming(),
                        dateStorage(), discriminator(), discriminatorKey(), enablePolymorphicQueries(), ignoreFinals(), mapper(),
                        packages(), propertyDiscovery(), propertyNaming(), queryFactory(), storeEmpties(), storeNulls());
    }

    @Override
    public String database() {
        return orDefault(database, "morphia");
    }

    @Override
    public Boolean applyCaps() {
        return orDefault(applyCaps, FALSE);
    }

    @Override
    public MorphiaConfig applyCaps(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.applyCaps = value;
        return newConfig;
    }

    @Override
    public Boolean applyDocumentValidations() {
        return orDefault(applyDocumentValidations, FALSE);
    }

    @Override
    public MorphiaConfig applyDocumentValidations(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.applyDocumentValidations = value;
        return newConfig;
    }

    @Override
    public Boolean applyIndexes() {
        return orDefault(applyIndexes, FALSE);
    }

    @Override
    public MorphiaConfig applyIndexes(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.applyIndexes = value;
        return newConfig;
    }

    @Override
    public Optional<CodecProvider> codecProvider() {
        return orDefault(codecProvider, Optional.empty());
    }

    @Override
    public MorphiaConfig codecProvider(CodecProvider value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.codecProvider = Optional.of(value);
        return newConfig;
    }

    @Override
    public NamingStrategy collectionNaming() {
        return orDefault(collectionNaming, camelCase());
    }

    @Override
    public MorphiaConfig collectionNaming(NamingStrategy value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.collectionNaming = value;
        return newConfig;
    }

    @Override
    public MorphiaConfig database(String value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.database = value;
        return newConfig;
    }

    @Override
    public DateStorage dateStorage() {
        return orDefault(dateStorage, UTC);
    }

    @Override
    public MorphiaConfig dateStorage(DateStorage value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.dateStorage = value;
        return newConfig;
    }

    @Override
    public DiscriminatorFunction discriminator() {
        return orDefault(discriminator, simpleName());
    }

    @Override
    public MorphiaConfig discriminator(DiscriminatorFunction value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.discriminator = value;
        return newConfig;
    }

    @Override
    public String discriminatorKey() {
        return orDefault(discriminatorKey, "_t");
    }

    @Override
    public MorphiaConfig discriminatorKey(String value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.discriminatorKey = value;
        return newConfig;
    }

    @Override
    public Boolean enablePolymorphicQueries() {
        return orDefault(enablePolymorphicQueries, FALSE);
    }

    @Override
    public MorphiaConfig enablePolymorphicQueries(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.enablePolymorphicQueries = value;
        return newConfig;
    }

    @Override
    public Boolean ignoreFinals() {
        return orDefault(ignoreFinals, FALSE);
    }

    @Override
    public MorphiaConfig ignoreFinals(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.ignoreFinals = value;
        return newConfig;
    }

    @Override
    public MorphiaConfig legacy() {
        ManualMorphiaConfig newConfig = new ManualMorphiaConfig(this);
        newConfig.dateStorage = DateStorage.SYSTEM_DEFAULT;
        newConfig.discriminatorKey = "className";
        newConfig.discriminator = className();
        newConfig.collectionNaming = identity();
        newConfig.propertyNaming = identity();
        return newConfig;
    }

    @Override
    public MapperType mapper() {
        return orDefault(mapper, MapperType.LEGACY);
    }

    @Override
    public MorphiaConfig mapper(MapperType value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.mapper = value;
        return newConfig;
    }

    @Override
    public List<String> packages() {
        return orDefault(packages, List.of());
    }

    @Override
    public MorphiaConfig packages(List<String> value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.packages = value;
        return newConfig;
    }

    @Override
    public List<PropertyAnnotationProvider<?>> propertyAnnotationProviders() {
        return orDefault(propertyAnnotationProviders, List.of(new MorphiaPropertyAnnotationProvider()));
    }

    @Override
    public MorphiaConfig propertyAnnotationProviders(List<PropertyAnnotationProvider<?>> list) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.propertyAnnotationProviders = list;
        if (list.isEmpty() || list.stream().noneMatch(p -> p instanceof MorphiaPropertyAnnotationProvider)) {
            newConfig.propertyAnnotationProviders.add(new MorphiaPropertyAnnotationProvider());
        }
        return newConfig;
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return orDefault(propertyDiscovery, FIELDS);
    }

    @Override
    public MorphiaConfig propertyDiscovery(PropertyDiscovery value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.propertyDiscovery = value;
        return newConfig;
    }

    @Override
    public NamingStrategy propertyNaming() {
        return orDefault(propertyNaming, identity());
    }

    @Override
    public MorphiaConfig propertyNaming(NamingStrategy value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.propertyNaming = value;
        return newConfig;
    }

    @Override
    public QueryFactory queryFactory() {
        return orDefault(queryFactory, new DefaultQueryFactory());
    }

    @Override
    public MorphiaConfig queryFactory(QueryFactory value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.queryFactory = value;
        return newConfig;
    }

    @Override
    public Boolean storeEmpties() {
        return orDefault(storeEmpties, FALSE);
    }

    @Override
    public MorphiaConfig storeEmpties(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.storeEmpties = value;
        return newConfig;
    }

    @Override
    public Boolean storeNulls() {
        return orDefault(storeNulls, FALSE);
    }

    @Override
    public MorphiaConfig storeNulls(Boolean value) {
        var newConfig = new ManualMorphiaConfig(this);
        newConfig.storeNulls = value;
        return newConfig;
    }

    @Override
    public String toConfigFormat(boolean showComplete) {
        return dumpConfigurationFile(this, showComplete);
    }

    protected <T> T orDefault(@Nullable T localValue, T defaultValue) {
        return localValue != null ? localValue : defaultValue;
    }
}
