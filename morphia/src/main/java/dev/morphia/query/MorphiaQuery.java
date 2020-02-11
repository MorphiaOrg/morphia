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
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;
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
import static dev.morphia.query.experimental.filters.Filters.text;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class MorphiaQuery<T> implements Query<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaQuery.class);
    private final DatastoreImpl datastore;
    private final Class<T> clazz;
    private final Mapper mapper;
    private final Document seedQuery;
    private boolean validate = true;
    private String collectionName;
    private MongoCollection<T> collection;
    private List<Filter> filters = new ArrayList<>();

    public MorphiaQuery(final Class<T> clazz, final Datastore datastore) {
        this(clazz, null, datastore);
    }

    public MorphiaQuery(final Class<T> clazz, final Document query, final Datastore datastore) {
        this.clazz = clazz;
        this.datastore = (DatastoreImpl) datastore;
        this.seedQuery = query;
        mapper = this.datastore.getMapper();
    }

    static <V> V legacyOperation() {
        throw new UnsupportedOperationException(Sofia.legacyQueryOperation());
    }

    static <V> V modernOperation() {
        throw new UnsupportedOperationException(Sofia.notAvailableInLegacy());
    }

/*
    @Override
    public CriteriaContainer and(final Criteria... criteria) {
        List<Filter> collect = collect(criteria);
        filters.remove(asList(criteria));
        filter(Filters.and(collect.toArray(new Filter[0])));
        return this;
    }

    @Override
    public FieldEnd<? extends CriteriaContainer> criteria(final String field) {
        return new CriteriaFieldEnd(field);
    }
*/

    @Override
    public FieldEnd<? extends Query<T>> field(final String name) {
        return new MorphiaQueryFieldEnd(name);
    }

    @Override
    public Query<T> filter(final String condition, final Object value) {
        final String[] parts = condition.trim().split(" ");
        if (parts.length < 1 || parts.length > 6) {
            throw new IllegalArgumentException("'" + condition + "' is not a legal filter condition");
        }

        final FilterOperator op = (parts.length == 2) ? FilterOperator.fromString(parts[1]) : FilterOperator.EQUAL;

        return filter(op.apply(parts[0].trim(), value));
    }

/*
    @Override
    public CriteriaContainer or(final Criteria... criteria) {
        List<Filter> collect = collect(criteria);
        filters.remove(asList(criteria));
        filter(Filters.or(collect.toArray(new Filter[0])));
        return this;
    }
*/

    @Override
    public Query filter(final Filter... additional) {
        for (final Filter filter : additional) {
            filters.add(filter
                            .entityType(getEntityClass())
                            .isValidating(validate));
        }
        return this;
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
    public Map<String, Object> explain(final FindOptions options) {
        return new LinkedHashMap<>(datastore.getDatabase()
                                            .runCommand(new Document("explain",
                                                new Document("find", getCollection().getNamespace().getCollectionName())
                                                    .append("filter", getQueryDocument()))));
    }

    @Override
    public Query<T> search(final String searchText) {
        return filter(text(searchText));
    }

    @Override
    public Query<T> search(final String searchText, final String language) {
        return filter(text(searchText).language(language));
    }

    @Override
    public MorphiaKeyCursor<T> keys() {
        return keys(new FindOptions());
    }

    @Override
    public MorphiaKeyCursor<T> keys(final FindOptions options) {
        FindOptions includeId = new FindOptions().copy(options)
                                                 .projection()
                                                 .include("_id");

        return new MorphiaKeyCursor<>(prepareCursor(includeId,
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
        Document query = getQueryDocument();
        return session == null ? getCollection().countDocuments(query, options)
                               : getCollection().countDocuments(session, query, options);
    }

    @Override
    public MorphiaCursor<T> execute() {
        return this.execute(new FindOptions());
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
    public Modify<T> modify() {
        return new Modify<>(this, datastore, mapper, getEntityClass(), collection);
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

    /**
     * @return the logged query
     * @morphia.internal
     */
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

    /**
     * @return the entity {@link Class}.
     * @morphia.internal
     */
    public Class<T> getEntityClass() {
        return clazz;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, validate, getCollectionName());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MorphiaQuery)) {
            return false;
        }
        final MorphiaQuery<?> query20 = (MorphiaQuery<?>) o;
        return validate == query20.validate &&
               Objects.equals(clazz, query20.clazz) &&
               Objects.equals(getCollectionName(), query20.getCollectionName());
    }

    public Document getQueryDocument() {
        DocumentWriter writer = new DocumentWriter(seedQuery);
        writer.writeStartDocument();
        EncoderContext context = EncoderContext.builder().build();
        for (Filter filter : filters) {
            filter.encode(mapper, writer, context);
        }
        writer.writeEndDocument();

        return writer.getDocument();
    }

/*
    private List<Filter> collect(final Criteria[] criteria) {
        List<Filter> collected = new ArrayList<>();
        for (final Criteria criterion : criteria) {
            List<Filter> list;
            if (criterion instanceof MorphiaQuery) {
                list = ((MorphiaQuery) criterion).filters;
            } else {
                list = ((CriteriaFieldEnd) criterion).getFilters();
            }
            if (list.size() > 1) {
                collected.add(Filters.and(list.toArray(new Filter[0])));
            } else if (list.size() == 1) {
                collected.add(list.get(0));
            }
        }
        return collected;
    }
*/

    /**
     * @return the collection this query targets
     * @morphia.internal
     */
    private MongoCollection<T> getCollection() {
        if (collection == null) {
            collection = mapper.getCollection(clazz);
        }
        return collection;
    }

    private String getCollectionName() {
        if (collectionName == null) {
            collectionName = getCollection().getNamespace().getCollectionName();
        }
        return collectionName;
    }

    /**
     * Converts the query to a Document and updates for any discriminator values as my be necessary
     *
     * @return the query
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
                       .apply(iterable, mapper, clazz)
                       .iterator();
        } finally {
            if (findOptions.isLogQuery()) {
                datastore.getDatabase().runCommand(new Document("profile", oldProfile.get("was"))
                                                       .append("slowms", oldProfile.get("slowms"))
                                                       .append("sampleRate", oldProfile.get("sampleRate")));
            }

        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MorphiaQuery.class.getSimpleName() + "[", "]")
                   .add("clazz=" + clazz.getSimpleName())
                   .add("query=" + getQueryDocument())
                   .toString();
    }

    @Override
    public Query<T> where(final String js) {
        filter(Filters.where(js));
        return this;
    }

    private class CriteriaFieldEnd extends FieldEndImpl<CriteriaFieldEnd> implements CriteriaContainer {
        private final String name;
        private List<Filter> filters = new ArrayList<>();

        private CriteriaFieldEnd(final String name) {
            super(mapper, name, null, mapper.getMappedClass(getEntityClass()), validate);
            this.name = name;
        }

        @Override
        public void add(final Criteria... criteria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CriteriaContainer and(final Criteria... criteria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FieldEnd<? extends CriteriaContainer> criteria(final String field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CriteriaContainer or(final Criteria... criteria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(final Criteria criteria) {
            throw new UnsupportedOperationException();
        }

        public List<Filter> getFilters() {
            return filters;
        }

        @Override
        public Document toDocument() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void attach(final CriteriaContainer container) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFieldName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", CriteriaFieldEnd.class.getSimpleName() + "[", "]")
                       .add("name='" + name + "'")
                       .add("filters=" + filters)
                       .toString();
        }

        @Override
        protected CriteriaFieldEnd addCriteria(final FilterOperator op, final Object val, final boolean not) {
            filters.add(op.apply(name, val));
            return this;
        }
    }

    private class MorphiaQueryFieldEnd extends FieldEndImpl {
        private final String name;

        private MorphiaQueryFieldEnd(final String name) {
            super(mapper, name, MorphiaQuery.this, mapper.getMappedClass(getEntityClass()), validate);
            this.name = name;
        }

        @Override
        protected MorphiaQuery addCriteria(final FilterOperator op, final Object val, final boolean not) {
            Filter converted = op.apply(name, val);
            if(not) {
                converted.not();
            }
            filter(converted);
            return MorphiaQuery.this;
        }
    }
}
