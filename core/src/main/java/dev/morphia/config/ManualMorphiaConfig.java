package dev.morphia.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

import static dev.morphia.mapping.DateStorage.UTC;
import static dev.morphia.mapping.DiscriminatorFunction.simpleName;
import static dev.morphia.mapping.NamingStrategy.camelCase;
import static dev.morphia.mapping.NamingStrategy.identity;
import static java.lang.Boolean.FALSE;

/**
 * @since 2.4
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
@SuppressWarnings("removal")
public class ManualMorphiaConfig implements MorphiaConfig {
    Boolean applyCaps;
    Boolean applyDocumentValidations;
    Boolean applyIndexes;

    Optional<CodecProvider> codecProvider;

    NamingStrategy collectionNaming;

    String database;

    DateStorage dateStorage;
    DiscriminatorFunction discriminator;
    String discriminatorKey;
    Boolean enablePolymorphicQueries;
    Boolean ignoreFinals;
    MapperType mapper;
    List<String> packages;
    PropertyDiscovery propertyDiscovery;
    List<PropertyAnnotationProvider<?>> propertyAnnotationProviders;
    NamingStrategy propertyNaming;
    QueryFactory queryFactory;
    Boolean storeEmpties;
    Boolean storeNulls;

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public ManualMorphiaConfig() {
    }

    /**
     * @param base the base configuration to copy values from
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
        propertyAnnotationProviders = base.propertyAnnotationProviders().stream()
                .filter(p -> !(p instanceof MorphiaPropertyAnnotationProvider))
                .collect(Collectors.toCollection(ArrayList::new));
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
    public MapperType mapper() {
        return orDefault(mapper, MapperType.REFLECTION);
    }

    @Override
    public List<String> packages() {
        return orDefault(packages, List.of());
    }

    @Override
    public List<PropertyAnnotationProvider<?>> propertyAnnotationProviders() {
        var providers = new ArrayList<PropertyAnnotationProvider<?>>(List.of(new MorphiaPropertyAnnotationProvider()));
        if (propertyAnnotationProviders != null) {
            providers.addAll(propertyAnnotationProviders);
        }
        return providers;
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return orDefault(propertyDiscovery, PropertyDiscovery.FIELDS);
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

    protected <T> T orDefault(@Nullable T localValue, T defaultValue) {
        return localValue != null ? localValue : defaultValue;
    }

    /**
     * A hash of this config's mapping-relevant fields — the ones that affect the
     * structure/validation of the {@code EntityModel} graph a {@code Mapper} builds
     * (packages, mapper type, naming strategies, discriminator settings, etc.) — for use as
     * a cache key by callers that want to reuse an already-mapped {@code Mapper} across
     * configs that are equivalent for mapping purposes.
     * <p>
     * Deliberately excludes fields that only affect datastore-operational behavior and vary
     * independently of mapping structure, notably {@link #database()} (e.g. tests typically
     * derive a unique database name per test class from otherwise-identical mapping config),
     * along with {@link #applyCaps()}, {@link #applyIndexes()}, {@link #applyDocumentValidations()},
     * {@link #queryFactory()}, and {@link #codecProvider()}.
     * <p>
     * Strategy/function fields ({@link #collectionNaming()}, {@link #propertyNaming()},
     * {@link #discriminator()}, {@link #propertyAnnotationProviders()}) are hashed by
     * implementation class name rather than by instance, since their factory methods
     * (e.g. {@code NamingStrategy.camelCase()}) return a fresh instance on every call with
     * no {@code equals}/{@code hashCode} override — hashing the instances directly would make
     * this key non-deterministic even for two configs with the same nominal strategy.
     *
     * @return a hash of the mapping-relevant fields
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public int mappingCacheKey() {
        return Objects.hash(
                packages(),
                mapper(),
                collectionNaming().getClass().getName(),
                propertyNaming().getClass().getName(),
                discriminator().getClass().getName(),
                discriminatorKey(),
                enablePolymorphicQueries(),
                ignoreFinals(),
                propertyDiscovery(),
                storeEmpties(),
                storeNulls(),
                dateStorage(),
                propertyAnnotationProviders().stream()
                        .map(p -> p.getClass().getName())
                        .sorted()
                        .toList());
    }
}
