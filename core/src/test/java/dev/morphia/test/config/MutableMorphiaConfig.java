package dev.morphia.test.config;

import java.util.Optional;

import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.DiscriminatorFunction;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.query.LegacyQueryFactory;
import dev.morphia.query.QueryFactory;

import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;

@SuppressWarnings("removal")
public class MutableMorphiaConfig implements MorphiaConfig {
    private String database;
    private Optional<CodecProvider> codecProvider;
    private NamingStrategy collectionNaming;
    private DateStorage dateStorage;
    private DiscriminatorFunction discriminator;
    private String discriminatorKey;
    private boolean enablePolymorphicQueries;
    private boolean ignoreFinals;
    private boolean mapSubpackages;
    private PropertyDiscovery propertyDiscovery;
    private NamingStrategy propertyNaming;
    private QueryFactory queryFactory;
    private boolean storeEmpties;
    private boolean storeNulls;
    private UuidRepresentation uuidRepresentation;

    public MutableMorphiaConfig(MorphiaConfig base) {
        codecProvider = base.codecProvider();
        collectionNaming = base.collectionNaming();
        database = base.database();
        dateStorage = base.dateStorage();
        discriminator = base.discriminator();
        discriminatorKey = base.discriminatorKey();
        enablePolymorphicQueries = base.enablePolymorphicQueries();
        ignoreFinals = base.ignoreFinals();
        mapSubpackages = base.mapSubpackages();
        propertyDiscovery = base.propertyDiscovery();
        propertyNaming = base.propertyNaming();
        queryFactory = base.queryFactory();
        storeEmpties = base.storeEmpties();
        storeNulls = base.storeNulls();
        uuidRepresentation = base.uuidRepresentation();
    }

    @Override
    public Optional<CodecProvider> codecProvider() {
        return codecProvider;
    }

    public MutableMorphiaConfig codecProvider(CodecProvider codecProvider) {
        this.codecProvider = Optional.of(codecProvider);
        return this;
    }

    @Override
    public NamingStrategy collectionNaming() {
        return collectionNaming;
    }

    public MutableMorphiaConfig collectionNaming(NamingStrategy collectionNaming) {
        this.collectionNaming = collectionNaming;
        return this;
    }

    @Override
    public String database() {
        return database;
    }

    public MutableMorphiaConfig database(String database) {
        this.database = database;
        return this;
    }

    @Override
    public DateStorage dateStorage() {
        return dateStorage;
    }

    public MutableMorphiaConfig dateStorage(DateStorage dateStorage) {
        this.dateStorage = dateStorage;
        return this;
    }

    @Override
    public DiscriminatorFunction discriminator() {
        return discriminator;
    }

    public MutableMorphiaConfig discriminator(DiscriminatorFunction discriminator) {
        this.discriminator = discriminator;
        return this;
    }

    @Override
    public String discriminatorKey() {
        return discriminatorKey;
    }

    public MutableMorphiaConfig discriminatorKey(String discriminatorKey) {
        this.discriminatorKey = discriminatorKey;
        return this;
    }

    @Override
    public boolean enablePolymorphicQueries() {
        return enablePolymorphicQueries;
    }

    public MutableMorphiaConfig enablePolymorphicQueries(boolean enablePolymorphicQueries) {
        this.enablePolymorphicQueries = enablePolymorphicQueries;
        return this;
    }

    @Override
    public boolean ignoreFinals() {
        return ignoreFinals;
    }

    public MutableMorphiaConfig ignoreFinals(boolean ignoreFinals) {
        this.ignoreFinals = ignoreFinals;
        return this;
    }

    public MorphiaConfig legacy() {

        dateStorage(DateStorage.SYSTEM_DEFAULT);
        discriminatorKey("className");
        discriminator(DiscriminatorFunction.className());
        collectionNaming(NamingStrategy.identity());
        propertyNaming(NamingStrategy.identity());
        queryFactory(new LegacyQueryFactory());

        return this;
    }

    @Override
    public boolean mapSubpackages() {
        return mapSubpackages;
    }

    public MutableMorphiaConfig mapSubpackages(boolean mapSubpackages) {
        this.mapSubpackages = mapSubpackages;
        return this;
    }

    @Override
    public PropertyDiscovery propertyDiscovery() {
        return propertyDiscovery;
    }

    public MutableMorphiaConfig propertyDiscovery(PropertyDiscovery propertyDiscovery) {
        this.propertyDiscovery = propertyDiscovery;
        return this;
    }

    @Override
    public NamingStrategy propertyNaming() {
        return propertyNaming;
    }

    public MutableMorphiaConfig propertyNaming(NamingStrategy propertyNaming) {
        this.propertyNaming = propertyNaming;
        return this;
    }

    @Override
    public QueryFactory queryFactory() {
        return queryFactory;
    }

    public MutableMorphiaConfig queryFactory(QueryFactory queryFactory) {
        this.queryFactory = queryFactory;
        return this;
    }

    @Override
    public boolean storeEmpties() {
        return storeEmpties;
    }

    public MutableMorphiaConfig storeEmpties(boolean storeEmpties) {
        this.storeEmpties = storeEmpties;
        return this;
    }

    @Override
    public boolean storeNulls() {
        return storeNulls;
    }

    public MutableMorphiaConfig storeNulls(boolean storeNulls) {
        this.storeNulls = storeNulls;
        return this;
    }

    @Override
    public UuidRepresentation uuidRepresentation() {
        return uuidRepresentation;
    }

    public MutableMorphiaConfig uuidRepresentation(UuidRepresentation uuidRepresentation) {
        this.uuidRepresentation = uuidRepresentation;
        return this;
    }
}
