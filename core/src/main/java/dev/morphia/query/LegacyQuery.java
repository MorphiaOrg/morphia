package dev.morphia.query;


import com.mongodb.ExplainVerbosity;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.DatastoreImpl;
import dev.morphia.DeleteOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.internal.MorphiaInternals.DriverVersion;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.query.internal.MorphiaKeyCursor;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mongodb.CursorType.NonTailable;
import static dev.morphia.internal.MorphiaInternals.tryInvoke;
import static dev.morphia.query.CriteriaJoin.AND;
import static java.lang.String.format;


/**
 * Implementation of Query
 *
 * @param <T> The type we will be querying for, and returning.
 * @morphia.internal
 */
@SuppressWarnings("removal")
public
class LegacyQuery<T> implements CriteriaContainer, Query<T> {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyQuery.class);
    private final DatastoreImpl datastore;
    private final Class<T> clazz;
    private final Mapper mapper;
    private final String collectionName;
    private final MongoCollection<T> collection;
    private final EntityModel model;
    private boolean validateName = true;
    private boolean validateType = true;
    private Document baseQuery;
    @Deprecated
    private FindOptions options;
    private final CriteriaContainer compoundContainer;

    /**
     * Creates a Query for the given type and collection
     *
     * @param datastore the Datastore to use
     * @param clazz     the type to return
     */
    protected LegacyQuery(Datastore datastore, @Nullable String collectionName, Class<T> clazz) {
        this.clazz = clazz;
        this.datastore = (DatastoreImpl) datastore;
        mapper = this.datastore.getMapper();
        model = mapper.getEntityModel(clazz);
        if (collectionName != null) {
            this.collection = datastore.getDatabase().getCollection(collectionName, clazz);
            this.collectionName = collectionName;
        } else {
            this.collection = mapper.getCollection(clazz);
            this.collectionName = this.collection.getNamespace().getCollectionName();
        }

        compoundContainer = new CriteriaContainerImpl(mapper, this, AND);
    }

    @Override
    public void add(Criteria... criteria) {
        for (Criteria c : criteria) {
            c.attach(this);
            compoundContainer.add(c);
        }
    }

    @Override
    public CriteriaContainer and(Criteria... criteria) {
        return compoundContainer.and(criteria);
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(String field) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(mapper, this, AND);
        add(container);

        return new FieldEndImpl<CriteriaContainer>(mapper, field, container, model, this.isValidatingNames());
    }

    @Override
    public CriteriaContainer or(Criteria... criteria) {
        return compoundContainer.or(criteria);
    }

    @Override
    public void remove(Criteria criteria) {
        compoundContainer.remove(criteria);
    }

    @Override
    public void attach(CriteriaContainer container) {
        compoundContainer.attach(container);
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException("this method is unused on a Query");
    }

    /**
     * Execute the query and get the results.
     *
     * @return a MorphiaCursor
     * @see #iterator(FindOptions)
     */
    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public MorphiaCursor<T> execute() {
        return iterator();
    }

    @Override
    public long count() {
        return count(new CountOptions());
    }

    @Override
    public long count(CountOptions options) {
        ClientSession session = datastore.findSession(options);
        return session == null ? getCollection().countDocuments(getQueryDocument(), options)
                               : getCollection().countDocuments(session, getQueryDocument(), options);
    }

    @Override
    public DeleteResult delete(DeleteOptions options) {
        MongoCollection<T> collection = options.prepare(getCollection());
        ClientSession session = datastore.findSession(options);
        if (options.isMulti()) {
            return session == null
                   ? collection.deleteMany(getQueryDocument(), options)
                   : collection.deleteMany(session, getQueryDocument(), options);
        } else {
            return session == null
                   ? collection.deleteOne(getQueryDocument(), options)
                   : collection.deleteOne(session, getQueryDocument(), options);
        }
    }

    @Override
    public Query<T> disableValidation() {
        validateName = false;
        validateType = false;
        return this;
    }

    @Override
    public Query<T> enableValidation() {
        validateName = true;
        validateType = true;
        return this;
    }

    @Override
    public T findAndDelete(FindAndDeleteOptions options) {
        MongoCollection<T> mongoCollection = options.prepare(getCollection());
        ClientSession session = datastore.findSession(options);
        return session == null
               ? mongoCollection.findOneAndDelete(getQueryDocument(), options)
               : mongoCollection.findOneAndDelete(session, getQueryDocument(), options);
    }

    /**
     * Execute the query and get the results.
     *
     * @param options the options to apply to the find operation
     * @return a MorphiaCursor
     */
    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public MorphiaCursor<T> execute(FindOptions options) {
        return iterator(options);
    }

    @Override
    public Map<String, Object> explain(FindOptions options, @Nullable ExplainVerbosity verbosity) {
        return tryInvoke(DriverVersion.v4_2_0,
            () -> {
                return verbosity == null
                       ? iterable(options, collection).explain()
                       : iterable(options, collection).explain(verbosity);
            },
            () -> {
                return new LinkedHashMap<>(datastore.getDatabase()
                                                    .runCommand(new Document("explain",
                                                        new Document("find", getCollection().getNamespace().getCollectionName())
                                                            .append("filter", getQueryDocument()))));
            });
    }

    @Override
    public FieldEnd<? extends Query<T>> field(String name) {
        return new FieldEndImpl<>(mapper, name, this, model, this.isValidatingNames());
    }

    @Override
    public Query<T> filter(String condition, Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final String prop = parts[0].trim();
        final FilterOperator op = (parts.length == 2) ? translate(parts[1]) : FilterOperator.EQUAL;

        add(new FieldCriteria(mapper, prop, op, value, mapper.getEntityModel(this.getEntityClass()), this.isValidatingNames()));

        return this;
    }

    @Override
    public Modify<T> modify(UpdateOperator first, UpdateOperator... updates) {
        return new Modify<>(datastore, mapper, getCollection(), this, clazz, first, updates);
    }

    @Override
    public T first() {
        return first(new FindOptions());
    }

    @Override
    public T first(FindOptions options) {
        try (MongoCursor<T> it = iterator(options.copy().limit(1))) {
            return it.tryNext();
        }
    }

    /**
     * @return the entity {@link Class}.
     * @morphia.internal
     */
    @Override
    public Class<T> getEntityClass() {
        return clazz;
    }

    @Override
    public MorphiaCursor<T> iterator(FindOptions options) {
        return new MorphiaCursor<>(prepareCursor(options, getCollection()));
    }

    @Override
    public MorphiaKeyCursor<T> keys() {
        return keys(new FindOptions());
    }

    @Override
    public MorphiaKeyCursor<T> keys(FindOptions options) {
        FindOptions returnKey = new FindOptions().copy(options)
                                                 .projection()
                                                 .include("_id");

        return new MorphiaKeyCursor<>(prepareCursor(returnKey,
            datastore.getDatabase().getCollection(getCollectionName())), datastore.getMapper(),
            clazz, getCollectionName());
    }

    @Override
    public Modify<T> modify(UpdateOperations<T> operations) {
        return new Modify<>(datastore, mapper, getCollection(), this, clazz, (UpdateOpsImpl) operations);
    }

    @Override
    public Update<T> update(UpdateOperator first, UpdateOperator... updates) {
        return new Update<>(datastore, mapper, getCollection(), this, clazz, first, updates);
    }

    @Override
    public Query<T> retrieveKnownFields() {
        getOptions().projection().knownFields();
        return this;
    }

    @Override
    public Query<T> search(String search) {
        this.criteria("$text").equal(new Document("$search", search));
        return this;
    }

    @Override
    public Query<T> search(String search, String language) {
        this.criteria("$text").equal(new Document("$search", search)
                                         .append("$language", language));
        return this;
    }

    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public Update<T> update(UpdateOperations<T> operations) {
        return new Update<>(datastore, mapper, getCollection(), this, clazz, (UpdateOpsImpl<T>) operations);
    }

    /**
     * @return the Mongo fields {@link Document}.
     * @morphia.internal
     */
    @Nullable
    public Document getFieldsObject() {
        Projection projection = getOptions().getProjection();

        return projection != null ? projection.map(mapper, clazz) : null;
    }

    /**
     * @return the collection this query targets
     * @morphia.internal
     */
    public MongoCollection<T> getCollection() {
        return collection;
    }

    /**
     * @return the Mongo sort {@link Document}.
     * @morphia.internal
     */
    @Nullable
    public Document getSort() {
        return options != null ? options.getSort() : null;
    }

    /**
     * Converts the query to a Document and updates for any discriminator values as my be necessary
     *
     * @return the query
     * @morphia.internal
     */
    @Override
    public Document toDocument() {
        final Document query = getQueryDocument();
        EntityModel model = mapper.getEntityModel(getEntityClass());
        Entity entityAnnotation = model.getEntityAnnotation();
        if (entityAnnotation != null && entityAnnotation.useDiscriminator()
            && !query.containsKey("_id")
            && !query.containsKey(model.getDiscriminatorKey())) {

            List<EntityModel> subtypes = mapper.getEntityModel(getEntityClass()).getSubtypes();
            List<String> values = new ArrayList<>();
            values.add(model.getDiscriminator());
            for (EntityModel subtype : subtypes) {
                values.add(subtype.getDiscriminator());
            }
            query.put(model.getDiscriminatorKey(),
                new Document("$in", values));
        }
        return query;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, validateName, validateType, baseQuery, getOptions(), compoundContainer, getCollectionName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LegacyQuery)) {
            return false;
        }
        final LegacyQuery<?> query = (LegacyQuery<?>) o;
        return validateName == query.validateName
               && validateType == query.validateType
               && Objects.equals(clazz, query.clazz)
               && Objects.equals(baseQuery, query.baseQuery)
               && Objects.equals(getOptions(), query.getOptions())
               && Objects.equals(compoundContainer, query.compoundContainer)
               && Objects.equals(getCollectionName(), query.getCollectionName());
    }

    @Override
    public String toString() {
        return getOptions().getProjection() == null ? getQueryDocument().toString()
                                                    : format("{ %s, %s }", getQueryDocument(), getFieldsObject());
    }

    /**
     * @return true if field names are being validated
     */
    public boolean isValidatingNames() {
        return validateName;
    }

    /**
     * Sets query structure directly
     *
     * @param query the Document containing the query
     */
    public void setQueryObject(Document query) {
        baseQuery = new Document(query);
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    private String getCollectionName() {
        return collectionName;
    }

    private Document getQueryDocument() {
        final Document obj = new Document();

        if (baseQuery != null) {
            obj.putAll(baseQuery);
        }

        obj.putAll(compoundContainer.toDocument());
        mapper.updateQueryWithDiscriminators(mapper.getEntityModel(getEntityClass()), obj);
        return obj;
    }

    @NotNull
    private <E> FindIterable<E> iterable(FindOptions options, MongoCollection<E> collection) {
        final Document query = this.toDocument();

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Running query(%s) : %s, options: %s,", getCollectionName(), query.toJson(), options));
        }

        if ((options.getCursorType() != null && options.getCursorType() != NonTailable)
            && (options.getSort() != null)) {
            LOG.warn("Sorting on tail is not allowed.");
        }

        ClientSession clientSession = datastore.findSession(options);

        FindIterable<E> iterable = clientSession != null
                                   ? collection.find(clientSession, query)
                                   : collection.find(query);
        return iterable;
    }

    private <E> MongoCursor<E> prepareCursor(FindOptions options, MongoCollection<E> collection) {
        Document oldProfile = null;
        if (options.isLogQuery()) {
            oldProfile = datastore.getDatabase().runCommand(new Document("profile", 2).append("slowms", 0));
        }
        try {
            return options
                       .apply(iterable(options, collection), mapper, clazz)
                       .iterator();
        } finally {
            if (options.isLogQuery()) {
                datastore.getDatabase().runCommand(new Document("profile", oldProfile.get("was"))
                                                       .append("slowms", oldProfile.get("slowms"))
                                                       .append("sampleRate", oldProfile.get("sampleRate")));
            }

        }
    }

    /**
     * Converts the textual operator (">", "<=", etc) into a FilterOperator. Forgiving about the syntax; != and <> are NOT_EQUAL, = and ==
     * are EQUAL.
     */
    private FilterOperator translate(String operator) {
        return FilterOperator.fromString(operator);
    }

    FindOptions getOptions() {
        if (options == null) {
            options = new FindOptions();
        }
        return options;
    }
}
