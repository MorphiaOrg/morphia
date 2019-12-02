package dev.morphia.query;


import com.mongodb.WriteConcern;
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
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.CursorType.NonTailable;
import static dev.morphia.query.CriteriaJoin.AND;
import static java.lang.String.format;


/**
 * Implementation of Query
 *
 * @param <T> The type we will be querying for, and returning.
 */
@SuppressWarnings("removal")
public class QueryImpl<T> implements CriteriaContainer, Query<T> {
    private static final Logger LOG = LoggerFactory.getLogger(QueryImpl.class);
    private final DatastoreImpl datastore;
    private final Class<T> clazz;
    private final Mapper mapper;
    private boolean validateName = true;
    private boolean validateType = true;
    private Document baseQuery;
    private FindOptions options;
    private CriteriaContainer compoundContainer;
    private String collectionName;
    private MongoCollection<T> collection;
    private FindOptions previousOptions;

    /**
     * Creates a Query for the given type and collection
     *
     * @param clazz     the type to return
     * @param datastore the Datastore to use
     */
    public QueryImpl(final Class<T> clazz, final Datastore datastore) {
        this.clazz = clazz;
        this.datastore = (DatastoreImpl) datastore;
        mapper = this.datastore.getMapper();

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

    @Override
    public FieldEnd<? extends Query<T>> field(final String name) {
        return new FieldEndImpl<>(mapper, this, name, this);
    }

    @Override
    public Query<T> filter(final String condition, final Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final String prop = parts[0].trim();
        final FilterOperator op = (parts.length == 2) ? translate(parts[1]) : FilterOperator.EQUAL;

        add(new FieldCriteria(mapper, this, prop, op, value));

        return this;
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

        final Document op = new Document("$search", search);

        this.criteria("$text").equal(op);

        return this;
    }

    @Override
    public Query<T> search(final String search, final String language) {

        final Document op = new Document("$search", search)
                                .append("$language", language);

        this.criteria("$text").equal(op);

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
    public MorphiaKeyCursor<T> keys(final FindOptions options) {
        FindOptions returnKey = new FindOptions(options)
                                    .projection()
                                    .include("_id");

        return new MorphiaKeyCursor<>(prepareCursor(returnKey,
            datastore.getDatabase().getCollection(getCollectionName())), datastore.getMapper(),
            clazz, getCollectionName());
    }

    @Override
    public long count() {
        return count(new CountOptions());
    }

    @Override
    public long count(final CountOptions options) {
        ClientSession session = datastore.findSession(options);
        return session == null
        ? getCollection().countDocuments(getQueryDocument(), options)
               :getCollection().countDocuments(session, getQueryDocument(), options);
    }

    @Override
    public MorphiaCursor<T> execute() {
        return this.execute(getOptions());
    }

    @Override
    public MorphiaCursor<T> execute(final FindOptions options) {
        previousOptions = options;
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
        MongoCollection<T> mongoCollection = datastore.enforceWriteConcern(getCollection(), clazz, options.writeConcern());
        ClientSession session = datastore.findSession(options);
        return session == null
               ? mongoCollection.findOneAndDelete(getQueryDocument(), options)
               : mongoCollection.findOneAndDelete(session, getQueryDocument(), options);
    }

    @Override
    public Modify<T> modify() {
        return new Modify<>(this);
    }

    @Override
    public Modify<T> modify(final UpdateOperations<T> operations) {
        Modify<T> modify = modify();
        modify.setOps(((UpdateOpsImpl) operations).getOps());

        return modify;
    }

    @Override
    public DeleteResult remove(final DeleteOptions options) {
        MongoCollection<T> collection = enforceWriteConcern(clazz, options.getWriteConcern());
        ClientSession session = datastore.findSession(options);
        if (session == null) {
            return options.isMulti()
                   ? collection.deleteMany(getQueryDocument(), options)
                   : collection.deleteOne(getQueryDocument(), options);
        } else {
            return options.isMulti()
                   ? collection.deleteMany(session, getQueryDocument(), options)
                   : collection.deleteOne(session, getQueryDocument(), options);
        }
    }

    @Override
    public Update<T> update() {
        return new Update<>(datastore, mapper, clazz, getCollection(), this);
    }

    @Override
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

    /**
     * @return the logged query
     * @morphia.internal
     */
    public String getLoggedQuery() {
        if (previousOptions != null && previousOptions.isLogQuery()) {
            String json = "{}";
            Document first = datastore.getDatabase()
                                      .getCollection("system.profile")
                                      .find(new Document("command.comment", "logged query: " + previousOptions.getQueryLogId()),
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

    /**
     * @return the collection this query targets
     * @morphia.internal
     */
    public MongoCollection<T> getCollection() {
        if (collection == null) {
            collection = datastore.getCollection(clazz);
        }
        return collection;
    }

    @Override
    public Document toDocument() {
        return compoundContainer.toDocument();
    }

    @Override
    public void attach(final CriteriaContainer container) {
        compoundContainer.attach(container);
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException("this method is unused on a Query");
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
    public FieldEnd<? extends CriteriaContainer> criteria(final String field) {
        final CriteriaContainerImpl container = new CriteriaContainerImpl(mapper, this, AND);
        add(container);

        return new FieldEndImpl<CriteriaContainer>(mapper, this, field, container);
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
     * @return the entity {@link Class}.
     * @morphia.internal
     */
    public Class<T> getEntityClass() {
        return clazz;
    }

    /**
     * Sets query structure directly
     *
     * @param query the Document containing the query
     */
    public void setQueryObject(final Document query) {
        baseQuery = new Document(query);
    }

    /**
     * @return the Mongo sort {@link Document}.
     * @morphia.internal
     */
    public Document getSort() {
        return options != null ? options.getSort() : null;
    }

    /**
     * @return true if field names are being validated
     */
    public boolean isValidatingNames() {
        return validateName;
    }

    @Override
    public int hashCode() {
        int result = getCollection() != null ? getCollection().hashCode() : 0;
        result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
        result = 31 * result + (validateName ? 1 : 0);
        result = 31 * result + (validateType ? 1 : 0);
        result = 31 * result + (baseQuery != null ? baseQuery.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        result = 31 * result + (compoundContainer != null ? compoundContainer.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryImpl)) {
            return false;
        }

        final QueryImpl<?> query = (QueryImpl<?>) o;

        if (validateName != query.validateName) {
            return false;
        }
        if (validateType != query.validateType) {
            return false;
        }
        if (getCollection() != null ? !getCollection().equals(query.getCollection()) : query.getCollection() != null) {
            return false;
        }
        if (clazz != null ? !clazz.equals(query.clazz) : query.clazz != null) {
            return false;
        }
        if (baseQuery != null ? !baseQuery.equals(query.baseQuery) : query.baseQuery != null) {
            return false;
        }
        if (options != null ? !options.equals(query.options) : query.options != null) {
            return false;
        }
        return compoundContainer != null ? compoundContainer.equals(query.compoundContainer) : query.compoundContainer == null;
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
     * Converts the query to a Document and updates for any discriminator values as my be necessary
     *
     * @return th query
     * @morphia.internal
     */
    public Document prepareQuery() {
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

    protected Datastore getDatastore() {
        return datastore;
    }

    private Document getQueryDocument() {
        final Document obj = new Document();

        if (baseQuery != null) {
            obj.putAll(baseQuery);
        }

        obj.putAll(toDocument());

        return obj;
    }

    private <E> MongoCursor<E> prepareCursor(final FindOptions findOptions, final MongoCollection<E> collection) {
        final Document query = prepareQuery();

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Running query(%s) : %s, options: %s,", getCollectionName(), query, findOptions));
        }

        if (findOptions.getCursorType() != NonTailable && (findOptions.getSort() != null)) {
            LOG.warn("Sorting on tail is not allowed.");
        }

        ClientSession clientSession = datastore.findSession(findOptions);

        FindIterable<E> iterable = clientSession != null
                                   ? collection.find(clientSession, query)
                                   : collection.find(query);

        Document oldProfile = null;
        if (findOptions.isLogQuery()) {
            oldProfile = datastore.getDatabase().runCommand(new Document("profile", 2).append("slowms", 0));
        }
        try {
            return findOptions
                       .apply(this, iterable, mapper, clazz)
                       .iterator();
        } finally {
            if (findOptions.isLogQuery()) {
                datastore.getDatabase().runCommand(new Document("profile", oldProfile.get("was"))
                                                       .append("slowms", oldProfile.get("slowms"))
                                                       .append("sampleRate", oldProfile.get("sampleRate")));
            }

        }
    }

    private String getCollectionName() {
        if (collectionName == null) {
            collectionName = getCollection().getNamespace().getCollectionName();
        }
        return collectionName;
    }

    private MongoCollection<T> enforceWriteConcern(final Class<T> klass, final WriteConcern writeConcern) {
        WriteConcern concern = writeConcern;
        if (concern == null) {
            concern = getWriteConcern(klass);
        }
        return concern == null ? getCollection() : getCollection().withWriteConcern(concern);
    }

    /**
     * Converts the textual operator (">", "<=", etc) into a FilterOperator. Forgiving about the syntax; != and <> are NOT_EQUAL, = and ==
     * are EQUAL.
     */
    private FilterOperator translate(final String operator) {
        return FilterOperator.fromString(operator);
    }

    FindOptions getOptions() {
        if (options == null) {
            options = new FindOptions();
        }
        return options;
    }

    WriteConcern getWriteConcern(final Class clazz) {
        WriteConcern wc = null;
        if (clazz != null) {
            final Entity entityAnn = mapper.getMappedClass(clazz).getEntityAnnotation();
            if (!entityAnn.concern().isEmpty()) {
                wc = WriteConcern.valueOf(entityAnn.concern());
            }
        }

        return wc;
    }

    class QueryDocument implements Bson {

        QueryDocument() {
        }

        @Override
        public <TDocument> BsonDocument toBsonDocument(final Class<TDocument> tDocumentClass, final CodecRegistry codecRegistry) {
            final Document obj = new Document();

            if (baseQuery != null) {
                obj.putAll(baseQuery);
            }

            obj.putAll(toDocument());

            return obj.toBsonDocument(Document.class, codecRegistry);
        }
    }
}
