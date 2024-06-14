package dev.morphia.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.NonNull;
import com.mongodb.lang.Nullable;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.MorphiaDatastore;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.sofia.Sofia;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.mapping.codec.CodecHelper.coalesce;
import static dev.morphia.mapping.codec.CodecHelper.document;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * @param <T> the type
 * @morphia.internal
 * @hidden
 */
@SuppressWarnings({ "removal", "deprecation" })
@MorphiaInternal
public class MorphiaQuery<T> implements Query<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MorphiaQuery.class);
    private final MorphiaDatastore datastore;
    private final Class<T> type;
    private final Mapper mapper;
    private final List<Filter> filters = new ArrayList<>();
    private final Document seedQuery;
    private String collectionName;
    private MongoCollection<T> collection;

    private ValidationException invalid;

    private boolean validate = true;
    private FindOptions lastOptions;

    /**
     * @param datastore      the datastore
     * @param collectionName the collection name
     * @param type           the type to query against
     */
    protected MorphiaQuery(Datastore datastore, @Nullable String collectionName, Class<T> type) {
        this.type = type;
        this.datastore = (MorphiaDatastore) datastore;
        mapper = this.datastore.getMapper();
        seedQuery = null;
        this.collectionName = collectionName;
        if (collectionName != null) {
            collection = datastore.getDatabase().getCollection(collectionName, type);
        } else if (mapper.isMappable(type)) {
            collection = datastore.getCollection(type);
            this.collectionName = collection.getNamespace().getCollectionName();
        }
    }

    /**
     * @param datastore the datastore
     * @param type      the type to query against
     * @param query     the query
     */
    protected MorphiaQuery(Datastore datastore, Class<T> type, @Nullable Document query) {
        this.type = type;
        this.datastore = (MorphiaDatastore) datastore;
        this.seedQuery = query;
        mapper = this.datastore.getMapper();
        collection = datastore.getCollection(type);
        collectionName = collection.getNamespace().getCollectionName();
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
    public long count() {
        return count(new CountOptions());
    }

    @Override
    public long count(CountOptions options) {
        MongoCollection<T> collection = datastore.configureCollection(options, this.collection);
        return datastore.operations().countDocuments(collection, getQueryDocument(), options);
    }

    @Override
    public DeleteResult delete(DeleteOptions options) {
        MongoCollection<T> collection = datastore.configureCollection(options, this.collection);
        if (options.multi()) {
            return datastore.operations().deleteMany(collection, getQueryDocument(), options);
        } else {
            return datastore.operations().deleteOne(collection, getQueryDocument(), options);
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
        return verbosity == null
                ? iterable(options, collection).explain()
                : iterable(options, collection).explain(verbosity);
    }

    @Override
    public String getLoggedQuery() {
        if (lastOptions != null && lastOptions.isLogQuery()) {
            String json = "{}";
            Document filter = new Document("command.comment", Sofia.loggedQuery(lastOptions.queryLogId()));
            Document first = datastore.getDatabase()
                    .getCollection("system.profile")
                    .find(filter, Document.class)
                    .projection(new Document("command.filter", 1))
                    .first();
            if (first != null) {
                Document command = (Document) first.get("command");
                filter = (Document) command.get("filter");
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
    public T findAndDelete(FindAndDeleteOptions options) {
        MongoCollection<T> mongoCollection = datastore.configureCollection(options, collection);
        return datastore.operations().findOneAndDelete(mongoCollection, getQueryDocument(), options);
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

    public Class<T> getEntityClass() {
        return type;
    }

    @Override
    public T modify(ModifyOptions options, UpdateOperator first, UpdateOperator... updates) {
        EntityModel entityModel = mapper.getEntityModel(getEntityClass());

        Operations value = new Operations(entityModel, coalesce(first, updates), validate);

        return datastore.operations().findOneAndUpdate(datastore.configureCollection(options, collection),
                toDocument(), value.toDocument(datastore), options);
    }

    @MorphiaInternal
    public boolean isValidate() {
        return validate;
    }

    @Override
    public MorphiaCursor<T> iterator(@Nullable FindOptions options) {
        return new MorphiaCursor<>(prepareCursor(options != null ? options : new FindOptions(), collection));
    }

    /**
     * Converts the query to a Document and updates for any discriminator values as my be necessary
     *
     * @return the query
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Document toDocument() {
        return getQueryDocument();
    }

    @Override
    public UpdateResult update(UpdateOptions options, UpdateOperator first, UpdateOperator... updates) {
        if (invalid != null) {
            throw invalid;
        }
        EntityModel entityModel = mapper.getEntityModel(getEntityClass());
        Document updateOperations = new Operations(entityModel, coalesce(first, updates), isValidate())
                .toDocument(datastore);

        final Document queryObject = toDocument();
        if (options.isUpsert()) {
            if (entityModel.useDiscriminator()) {
                queryObject.put(entityModel.discriminatorKey(), entityModel.discriminator());
            }
        }

        MongoCollection<T> mongoCollection = options.prepare(collection, datastore.getDatabase());

        return options.multi() ? datastore.operations().updateMany(mongoCollection, queryObject, updateOperations, options)
                : datastore.operations().updateOne(mongoCollection, queryObject, updateOperations, options);
    }

    @Override
    public UpdateResult update(UpdateOptions options, Stage first, Stage... stages) {
        if (invalid != null) {
            throw invalid;
        }
        var updateOperations = coalesce(first, stages)
                .stream()
                .map(update -> {
                    DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());
                    Codec codec = datastore.getCodecRegistry().get(update.getClass());
                    codec.encode(writer, update, EncoderContext.builder().build());
                    return writer.getDocument();
                })
                .collect(toList());
        final Document queryObject = toDocument();

        MongoCollection<T> mongoCollection = datastore.configureCollection(options, datastore.configureCollection(options, collection));
        return options.multi()
                ? datastore.operations().updateMany(mongoCollection, queryObject, updateOperations, options)
                : datastore.operations().updateOne(mongoCollection, queryObject, updateOperations, options);
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

    private String getCollectionName() {
        return collectionName;
    }

    @NonNull
    private <E> FindIterable<E> iterable(FindOptions findOptions, MongoCollection<E> collection) {
        final Document query = toDocument();

        if (LOG.isTraceEnabled()) {
            LOG.trace(format("Running query(%s) : %s, options: %s,", getCollectionName(), query, findOptions));
        }

        MongoCollection<E> updated = datastore.configureCollection(findOptions, collection);

        return datastore.operations().find(updated, query);
    }

    @SuppressWarnings("ConstantConditions")
    private <E> MongoCursor<E> prepareCursor(FindOptions options, MongoCollection<E> collection) {
        Document oldProfile = null;
        lastOptions = options;
        if (options.isLogQuery()) {
            oldProfile = datastore.getDatabase().runCommand(new Document("profile", 2).append("slowms", 0));
        }
        options.disableValidation(!isValidate());
        try {
            return options
                    .apply(iterable(options, collection), mapper, type)
                    .iterator();
        } finally {
            if (options.isLogQuery()) {
                datastore.getDatabase().runCommand(new Document("profile", oldProfile.get("was"))
                        .append("slowms", oldProfile.get("slowms"))
                        .append("sampleRate", oldProfile.get("sampleRate")));
            }

        }
    }

    private Document getQueryDocument() {
        if (invalid != null) {
            throw invalid;
        }
        try {
            DocumentWriter writer = new DocumentWriter(mapper.getConfig(), seedQuery);
            document(writer, () -> {
                EncoderContext context = EncoderContext.builder().build();
                for (Filter filter : filters) {
                    Codec codec = datastore.getCodecRegistry().get(filter.getClass());
                    codec.encode(writer, filter, context);
                }
            });

            Document query = writer.getDocument();
            if (mapper.isMappable(getEntityClass())) {
                mapper.updateQueryWithDiscriminators(mapper.getEntityModel(getEntityClass()), query);
            }

            return query;
        } catch (ValidationException e) {
            invalid = e;
            throw e;
        }

    }

}
