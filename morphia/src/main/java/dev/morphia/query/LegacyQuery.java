package dev.morphia.query;


import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import dev.morphia.Datastore;
import dev.morphia.DatastoreImpl;
import dev.morphia.DeleteOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.query.internal.MorphiaKeyCursor;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mongodb.CursorType.NonTailable;
import static dev.morphia.query.CriteriaJoin.AND;
import static java.lang.String.format;


/**
 * Implementation of Query
 *
 * @param <T> The type we will be querying for, and returning.
 */
@SuppressWarnings("removal")
public class LegacyQuery<T> implements CriteriaContainer, Query<T> {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyQuery.class);
    private final DatastoreImpl datastore;
    private final Class<T> clazz;
    private final Mapper mapper;
    private boolean validateName = true;
    private boolean validateType = true;
    private Document baseQuery;
    @Deprecated
    private FindOptions options;
    private CriteriaContainer compoundContainer;
    private String collectionName;
    private MongoCollection<T> collection;
    private final MappedClass mappedClass;

    /**
     * Creates a Query for the given type and collection
     *
     * @param clazz     the type to return
     * @param datastore the Datastore to use
     */
    public LegacyQuery(final Class<T> clazz, final Datastore datastore) {
        this.clazz = clazz;
        this.datastore = (DatastoreImpl) datastore;
        mapper = this.datastore.getMapper();
        mappedClass = mapper.getMappedClass(clazz);

        compoundContainer = new CriteriaContainerImpl(mapper, this, AND);
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
    public Map<String, Object> explain(final FindOptions options) {
        return new LinkedHashMap<>(datastore.getDatabase()
                                            .runCommand(new Document("explain",
                                                new Document("find", getCollection().getNamespace().getCollectionName())
                                                    .append("filter", getQueryDocument()))));
    }

/*
    @Override
    public Query<T> expr(final Expression expression) {
        throw new UnsupportedOperationException();
    }
*/

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(final String field) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(mapper, this, AND);
        add(container);

        return new FieldEndImpl<CriteriaContainer>(mapper, field, container, mappedClass, this.isValidatingNames());
    }

    @Override
    public boolean equals(final Object o) {
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
    public Query<T> order(final Meta sort) {
        getOptions().sort(sort.toDatabase());

        return this;
    }

    @Override
    public Query<T> order(final Sort... sorts) {
        Document sortList = new Document();
        for (Sort sort : sorts) {
            String s = sort.getField();
            if (validateName) {
                s = new PathTarget(datastore.getMapper(), clazz, s).translatedPath();
            }
            sortList.put(s, sort.getOrder());
        }
        getOptions().sort(sortList);
        return this;
    }

    @Override
    public Query<T> project(final String field, final boolean include) {
        Projection projection = getOptions().projection();
        if (include) {
            projection.include(field);
        } else {
            projection.exclude(field);
        }

        return this;
    }

    @Override
    public Query<T> project(final String field, final ArraySlice slice) {
        getOptions().projection().project(field, slice);
        return this;
    }

    @Override
    public Query<T> project(final Meta meta) {
        getOptions().projection().project(meta);
        return this;
    }

    @Override
    public Query<T> retrieveKnownFields() {
        getOptions().projection().knownFields();
        return this;
    }

    @Override
    public Query<T> search(final String search) {
        this.criteria("$text").equal(new Document("$search", search));
        return this;
    }

    @Override
    public Query<T> search(final String search, final String language) {
        this.criteria("$text").equal(new Document("$search", search)
                                         .append("$language", language));
        return this;
    }

    @Override
    public Query<T> where(final String js) {
        add(new WhereCriteria(js));
        return this;
    }

    @Override
    public MorphiaKeyCursor<T> keys() {
        return keys(new FindOptions());
    }

    @Override
    public FieldEnd<? extends Query<T>> field(final String name) {
        return new FieldEndImpl<>(mapper, name, this, mappedClass, this.isValidatingNames());
    }

    @Override
    public long count() {
        return count(new CountOptions());
    }

    @Override
    public long count(final CountOptions options) {
        ClientSession session = datastore.findSession(options);
        return session == null ? getCollection().countDocuments(getQueryDocument(), options)
                               : getCollection().countDocuments(session, getQueryDocument(), options);
    }

    @Override
    public MorphiaCursor<T> execute() {
        return this.execute(getOptions());
    }

    @Override
    public MorphiaCursor<T> execute(final FindOptions options) {
        return new MorphiaCursor<>(prepareCursor(options, getCollection()));
    }

    @Override
    public T first() {
        return first(new FindOptions());
    }

    @Override
    public T first(final FindOptions options) {
        try (MongoCursor<T> it = this.execute(options.copy().limit(1))) {
            return it.tryNext();
        }
    }

    @Override
    public T delete(final FindAndDeleteOptions options) {
        MongoCollection<T> mongoCollection = options.apply(getCollection());
        ClientSession session = datastore.findSession(options);
        return session == null
               ? mongoCollection.findOneAndDelete(getQueryDocument(), options)
               : mongoCollection.findOneAndDelete(session, getQueryDocument(), options);
    }

    @Override
    public Query<T> filter(final String condition, final Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final String prop = parts[0].trim();
        final FilterOperator op = (parts.length == 2) ? translate(parts[1]) : FilterOperator.EQUAL;

        add(new FieldCriteria(mapper, prop, op, value, mapper.getMappedClass(this.getEntityClass()), this.isValidatingNames()));

        return this;
    }

    @Override
    public Modify<T> modify(final UpdateOperations<T> operations) {
        Modify<T> modify = modify();
        modify.setOps(((UpdateOpsImpl) operations).getOps());

        return modify;
    }

    @Override
    public DeleteResult remove(final DeleteOptions options) {
        MongoCollection<T> collection = options.apply(getCollection());
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
    public Update<T> update() {
        return new Update<>(datastore, mapper, clazz, getCollection(), this);
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public Update<T> update(final Document document) {
        final Update<T> updates = update();
        updates.setOps(document);

        return updates;
    }

    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public Update<T> update(final UpdateOperations operations) {
        final Update<T> updates = update();
        updates.setOps(((UpdateOpsImpl) operations).getOps());

        return updates;
    }

    @Override
    public MorphiaKeyCursor<T> keys(final FindOptions options) {
        FindOptions returnKey = new FindOptions().copy(options)
                                                 .projection()
                                                 .include("_id");

        return new MorphiaKeyCursor<>(prepareCursor(returnKey,
            datastore.getDatabase().getCollection(getCollectionName())), datastore.getMapper(),
            clazz, getCollectionName());
    }

    @Override
    public Modify<T> modify() {
        return new Modify<>(this, datastore, mapper, clazz, collection);
    }

    /**
     * Converts the query to a Document and updates for any discriminator values as my be necessary
     *
     * @return the query
     * @morphia.internal
     */
    public Document toDocument() {
        final Document query = getQueryDocument();
        MappedClass mappedClass = mapper.getMappedClass(getEntityClass());
        Entity entityAnnotation = mappedClass != null ? mappedClass.getEntityAnnotation() : null;
        if (entityAnnotation != null && entityAnnotation.useDiscriminator()
            && !query.containsKey("_id")
            && !query.containsKey(mappedClass.getEntityModel().getDiscriminatorKey())) {

            List<MappedClass> subtypes = mapper.getMappedClass(getEntityClass()).getSubtypes();
            List<String> values = new ArrayList<>();
            values.add(mappedClass.getEntityModel().getDiscriminator());
            for (final MappedClass subtype : subtypes) {
                values.add(subtype.getEntityModel().getDiscriminator());
            }
            query.put(mappedClass.getEntityModel().getDiscriminatorKey(),
                new Document("$in", values));
        }
        return query;
    }

    private String getCollectionName() {
        if (collectionName == null) {
            collectionName = getCollection().getNamespace().getCollectionName();
        }
        return collectionName;
    }

    /**
     * @return the entity {@link Class}.
     * @morphia.internal
     */
    public Class<T> getEntityClass() {
        return clazz;
    }

    FindOptions getOptions() {
        if (options == null) {
            options = new FindOptions();
        }
        return options;
    }

    /**
     * Converts the textual operator (">", "<=", etc) into a FilterOperator. Forgiving about the syntax; != and <> are NOT_EQUAL, = and ==
     * are EQUAL.
     */
    private FilterOperator translate(final String operator) {
        return FilterOperator.fromString(operator);
    }

    @Override
    public void add(final Criteria... criteria) {
        for (final Criteria c : criteria) {
            c.attach(this);
            compoundContainer.add(c);
        }
    }

    @Override
    public CriteriaContainer and(final Criteria... criteria) {
        return compoundContainer.and(criteria);
    }

    @Override
    public String getLoggedQuery(final FindOptions options) {
        if (options != null && options.isLogQuery()) {
            String json = "{}";
            Document first = datastore.getDatabase()
                                      .getCollection("system.profile")
                                      .find(new Document("command.comment", "logged query: " + options.getQueryLogId()),
                                          Document.class)
                                      .projection(new Document("command.filter", 1))
                                      .first();
            if (first != null) {
                Document command = (Document) first.get("command");
                Document filter = (Document) command.get("filter");
                if (filter != null) {
                    json = filter.toJson(mapper.getCodecRegistry().get(Document.class));
                }
            }
            return json;
        } else {
            throw new IllegalStateException(Sofia.queryNotLogged());
        }
    }

    @Override
    public CriteriaContainer or(final Criteria... criteria) {
        return compoundContainer.or(criteria);
    }

    @Override
    public void remove(final Criteria criteria) {
        compoundContainer.remove(criteria);
    }

    /**
     * @return the collection this query targets
     * @morphia.internal
     */
    public MongoCollection<T> getCollection() {
        if (collection == null) {
            collection = mapper.getCollection(clazz);
        }
        return collection;
    }

    private Document getQueryDocument() {
        final Document obj = new Document();

        if (baseQuery != null) {
            obj.putAll(baseQuery);
        }

        obj.putAll(compoundContainer.toDocument());

        return obj;
    }

    @Override
    public void attach(final CriteriaContainer container) {
        compoundContainer.attach(container);
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException("this method is unused on a Query");
    }

    /**
     * @return the Mongo sort {@link Document}.
     * @morphia.internal
     */
    public Document getSort() {
        return options != null ? options.getSort() : null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, validateName, validateType, baseQuery, getOptions(), compoundContainer, getCollectionName());
    }

    private <E> MongoCursor<E> prepareCursor(final FindOptions findOptions, final MongoCollection<E> collection) {
        final Document query = this.toDocument();

        FindOptions options = findOptions.copy().copy(getOptions());
        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Running query(%s) : %s, options: %s,", getCollectionName(), query, options));
        }

        if (options.getCursorType() != NonTailable && (options.getSort() != null)) {
            LOG.warn("Sorting on tail is not allowed.");
        }

        ClientSession clientSession = datastore.findSession(options);

        FindIterable<E> iterable = clientSession != null
                                   ? collection.find(clientSession, query)
                                   : collection.find(query);

        Document oldProfile = null;
        if (options.isLogQuery()) {
            oldProfile = datastore.getDatabase().runCommand(new Document("profile", 2).append("slowms", 0));
        }
        try {
            return options
                       .apply(iterable, mapper, clazz)
                       .iterator();
        } finally {
            if (options.isLogQuery()) {
                datastore.getDatabase().runCommand(new Document("profile", oldProfile.get("was"))
                                                       .append("slowms", oldProfile.get("slowms"))
                                                       .append("sampleRate", oldProfile.get("sampleRate")));
            }

        }
    }

    @Override
    public String toString() {
        return getOptions().getProjection() == null ? getQueryDocument().toString()
                                                    : format("{ %s, %s }", getQueryDocument(), getFieldsObject());
    }

    /**
     * @return the Mongo fields {@link Document}.
     * @morphia.internal
     */
    public Document getFieldsObject() {
        Projection projection = getOptions().getProjection();

        return projection != null ? projection.map(mapper, clazz) : null;
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
    public void setQueryObject(final Document query) {
        baseQuery = new Document(query);
    }

    protected Datastore getDatastore() {
        return datastore;
    }
}
