package dev.morphia.query;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.internal.MorphiaInternals.DriverVersion;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.filters.NearFilter;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.query.internal.MorphiaKeyCursor;
import dev.morphia.sofia.Sofia;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static com.mongodb.CursorType.NonTailable;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.internal.MorphiaInternals.tryInvoke;
import static dev.morphia.query.experimental.filters.Filters.text;
import static java.lang.String.format;

/**
 * @param <T> the type
 * @morphia.internal
 */
class MorphiaQuery<T> implements Query<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaQuery.class);
    private final Datastore datastore;
    private final Class<T> type;
    private final Mapper mapper;
    private final List<Filter> filters = new ArrayList<>();
    private final Document seedQuery;
    private String collectionName;
    private MongoCollection<T> collection;
    private boolean validate = true;
    private FindOptions lastOptions;

    protected MorphiaQuery(Datastore datastore, @Nullable String collectionName, Class<T> type) {
        this.type = type;
        this.datastore = datastore;
        mapper = this.datastore.getMapper();
        seedQuery = null;
        this.collectionName = collectionName;
        if (collectionName != null) {
            this.collection = datastore.getDatabase().getCollection(collectionName, type);
        } else if (mapper.isMappable(type)) {
            this.collection = datastore.getCollection(type);
            this.collectionName = this.collection.getNamespace().getCollectionName();
        }
    }

    protected MorphiaQuery(Datastore datastore, Class<T> type, @Nullable Document query) {
        this.type = type;
        this.datastore = datastore;
        this.seedQuery = query;
        mapper = this.datastore.getMapper();
        collection = datastore.getCollection(type);
        collectionName = collection.getNamespace().getCollectionName();
    }

    static <V> V legacyOperation() {
        throw new UnsupportedOperationException(Sofia.legacyOperation());
    }

    @Override
    public String getLoggedQuery() {
        if (lastOptions.isLogQuery()) {
            String json = "{}";
            Document first = datastore.getDatabase()
                                      .getCollection("system.profile")
                                      .find(new Document("command.comment", "logged query: " + lastOptions.getQueryLogId()),
                                          Document.class)
                                      .projection(new Document("command.filter", 1))
                                      .first();
            if (first != null) {
                Document command = (Document) first.get("command");
                Document filter = (Document) command.get("filter");
                if (filter != null) {
                    json = filter.toJson(datastore.getCodecRegistry().get(Document.class));
                }
            }
            return json;
        } else {
            throw new IllegalStateException(Sofia.queryNotLogged());
        }
    }

    @Override
    public long count() {
        return count(new CountOptions());
    }

    @Override
    public long count(CountOptions options) {
        ClientSession session = datastore.findSession(options);
        Document query = getQueryDocument();
        return session == null ? getCollection().countDocuments(query, options)
                               : getCollection().countDocuments(session, query, options);
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
        validate = false;
        return this;
    }

    @Override
    public Query<T> enableValidation() {
        validate = true;
        return this;
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
    @SuppressWarnings({"removal", "unchecked"})
    public FieldEnd<? extends Query<T>> field(String name) {
        return new MorphiaQueryFieldEnd(name);
    }

    @Override
    @SuppressWarnings({"removal"})
    public Query<T> filter(String condition, Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final FilterOperator op = (parts.length == 2) ? FilterOperator.fromString(parts[1]) : FilterOperator.EQUAL;

        return filter(op.apply(parts[0].trim(), value));
    }

    @Override
    public Query<T> filter(Filter... additional) {
        for (Filter filter : additional) {
            filters.add(filter
                .entityType(getEntityClass())
                .isValidating(validate));
        }
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

    @Override
    public Class<T> getEntityClass() {
        return type;
    }

    @Override
    public Modify<T> modify(UpdateOperator first, UpdateOperator... updates) {
        return new Modify<>(datastore, getCollection(), this, getEntityClass(), first, updates);
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
        FindOptions includeId = new FindOptions().copy(options)
                                                 .projection()
                                                 .include("_id");

        return new MorphiaKeyCursor<>(prepareCursor(includeId, datastore.getDatabase().getCollection(getCollectionName())),
            datastore, type, getCollectionName());
    }

    @Override
    public Query<T> search(String searchText) {
        return filter(text(searchText));
    }

    @Override
    public Query<T> search(String searchText, String language) {
        return filter(text(searchText).language(language));
    }

    /**
     * Converts the query to a Document and updates for any discriminator values as my be necessary
     *
     * @return the query
     * @morphia.internal
     */
    @Override
    public Document toDocument() {
        return getQueryDocument();
    }

    @Override
    public Update<T> update(UpdateOperator first, UpdateOperator... updates) {
        return new Update<>(datastore, getCollection(), this, type, first, updates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, validate, getCollectionName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MorphiaQuery)) {
            return false;
        }
        final MorphiaQuery<?> query20 = (MorphiaQuery<?>) o;
        return validate == query20.validate
               && Objects.equals(type, query20.type)
               && Objects.equals(getCollectionName(), query20.getCollectionName());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MorphiaQuery.class.getSimpleName() + "[", "]")
            .add("clazz=" + type.getSimpleName())
            .add("query=" + getQueryDocument())
            .toString();
    }

    /**
     * @return the collection this query targets
     * @morphia.internal
     */
    private MongoCollection<T> getCollection() {
        return collection;
    }

    private String getCollectionName() {
        return collectionName;
    }

    @NonNull
    private <E> FindIterable<E> iterable(FindOptions findOptions, MongoCollection<E> collection) {
        final Document query = toDocument();

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Running query(%s) : %s, options: %s,", getCollectionName(), query, findOptions));
        }

        if ((findOptions.getCursorType() != null && findOptions.getCursorType() != NonTailable)
            && (findOptions.getSort() != null)) {
            LOG.warn("Sorting on tail is not allowed.");
        }

        ClientSession clientSession = datastore.findSession(findOptions);

        MongoCollection<E> updated = findOptions.prepare(collection);

        FindIterable<E> iterable = clientSession != null
                                   ? updated.find(clientSession, query)
                                   : updated.find(query);
        return iterable;
    }

    @SuppressWarnings("ConstantConditions")
    private <E> MongoCursor<E> prepareCursor(FindOptions findOptions, MongoCollection<E> collection) {
        Document oldProfile = null;
        lastOptions = findOptions;
        if (findOptions.isLogQuery()) {
            oldProfile = datastore.getDatabase().runCommand(new Document("profile", 2).append("slowms", 0));
        }
        try {
            return findOptions
                .apply(iterable(findOptions, collection), mapper, type)
                .iterator();
        } finally {
            if (findOptions.isLogQuery()) {
                datastore.getDatabase().runCommand(new Document("profile", oldProfile.get("was"))
                    .append("slowms", oldProfile.get("slowms"))
                    .append("sampleRate", oldProfile.get("sampleRate")));
            }

        }
    }

    Document getQueryDocument() {
        DocumentWriter writer = new DocumentWriter(mapper, seedQuery);
        document(writer, () -> {
            EncoderContext context = EncoderContext.builder().build();
            for (Filter filter : filters) {
                filter.encode(datastore, writer, context);
            }
        });

        Document query = writer.getDocument();
        if (mapper.isMappable(getEntityClass())) {
            mapper.updateQueryWithDiscriminators(mapper.getEntityModel(getEntityClass()), query);
        }

        return query;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Deprecated(since = "2.0", forRemoval = true)
    private class MorphiaQueryFieldEnd extends FieldEndImpl {
        private final String name;

        private MorphiaQueryFieldEnd(String name) {
            super(datastore, name, MorphiaQuery.this, mapper.getEntityModel(getEntityClass()), validate);
            this.name = name;
        }

        @Override
        @SuppressWarnings("removal")
        public CriteriaContainer within(Shape shape) {
            Filter converted;
            if (shape instanceof dev.morphia.query.Shape.Center) {
                final dev.morphia.query.Shape.Center center = (dev.morphia.query.Shape.Center) shape;
                converted = Filters.center(getField(), center.getCenter(), center.getRadius());
            } else if (shape.getGeometry().equals("$box")) {
                Point[] points = shape.getPoints();
                converted = Filters.box(getField(), points[0], points[1]);
            } else if (shape.getGeometry().equals("$polygon")) {
                converted = Filters.polygon(getField(), shape.getPoints());
            } else {
                throw new UnsupportedOperationException(Sofia.conversionNotSupported(shape.getGeometry()));
            }
            if (isNot()) {
                converted.not();
            }
            filter(converted);
            return MorphiaQuery.this;
        }

        @Override
        @SuppressWarnings("removal")
        protected MorphiaQuery<T> addCriteria(FilterOperator op, Object val, boolean not) {
            Filter converted = op.apply(name, val);
            if (not) {
                converted.not();
            }
            filter(converted);
            return MorphiaQuery.this;
        }

        @Override
        @SuppressWarnings("removal")
        protected CriteriaContainer addGeoCriteria(FilterOperator op, Object val, Map opts) {
            NearFilter apply = (NearFilter) op.apply(name, val);
            apply.applyOpts(opts);
            filter(apply);
            return MorphiaQuery.this;
        }
    }
}
