package xyz.morphia.query;

import com.mongodb.CursorType;
import com.mongodb.DBObject;
import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.internal.MongoBatchCursorAdapter;
import com.mongodb.client.internal.OperationExecutor;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOptions;
import com.mongodb.internal.operation.SyncOperations;
import com.mongodb.lang.Nullable;
import com.mongodb.operation.BatchCursor;
import com.mongodb.operation.ReadOperation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import xyz.morphia.Key;
import xyz.morphia.query.internal.MongoIterableImpl;

import java.util.concurrent.TimeUnit;

import static com.mongodb.assertions.Assertions.notNull;

class MorphiaKeyIterable<T> extends MongoIterableImpl<Key<T>> implements FindIterable<Key<T>> {

    private final SyncOperations<DBObject> operations;

    private final Class<T> resultClass;
    private final com.mongodb.client.model.FindOptions findOptions;

    private Bson filter;

    MorphiaKeyIterable(@Nullable final ClientSession clientSession, final MongoNamespace namespace, final Class<DBObject> documentClass,
                     final Class<T> resultClass, final CodecRegistry codecRegistry, final ReadPreference readPreference,
                     final ReadConcern readConcern, final OperationExecutor executor, final Bson filter) {
        super(clientSession, executor, readConcern, readPreference);
        this.operations = new SyncOperations<DBObject>(namespace, documentClass, readPreference, codecRegistry);
        this.resultClass = notNull("resultClass", resultClass);
        this.filter = notNull("filter", filter);
        this.findOptions = new FindOptions();
    }

    @Override
    public MorphiaKeyIterable<T> filter(@Nullable final Bson filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> limit(final int limit) {
        findOptions.limit(limit);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> skip(final int skip) {
        findOptions.skip(skip);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> maxTime(final long maxTime, final TimeUnit timeUnit) {
        notNull("timeUnit", timeUnit);
        findOptions.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> maxAwaitTime(final long maxAwaitTime, final TimeUnit timeUnit) {
        notNull("timeUnit", timeUnit);
        findOptions.maxAwaitTime(maxAwaitTime, timeUnit);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> batchSize(final int batchSize) {
        super.batchSize(batchSize);
        findOptions.batchSize(batchSize);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> collation(@Nullable final Collation collation) {
        findOptions.collation(collation);
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MorphiaKeyIterable<T> modifiers(@Nullable final Bson modifiers) {
        findOptions.modifiers(modifiers);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> projection(@Nullable final Bson projection) {
        findOptions.projection(projection);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> sort(@Nullable final Bson sort) {
        findOptions.sort(sort);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> noCursorTimeout(final boolean noCursorTimeout) {
        findOptions.noCursorTimeout(noCursorTimeout);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> oplogReplay(final boolean oplogReplay) {
        findOptions.oplogReplay(oplogReplay);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> partial(final boolean partial) {
        findOptions.partial(partial);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> cursorType(final CursorType cursorType) {
        findOptions.cursorType(cursorType);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> comment(@Nullable final String comment) {
        findOptions.comment(comment);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> hint(@Nullable final Bson hint) {
        findOptions.hint(hint);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> max(@Nullable final Bson max) {
        findOptions.max(max);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> min(@Nullable final Bson min) {
        findOptions.min(min);
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MorphiaKeyIterable<T> maxScan(final long maxScan) {
        findOptions.maxScan(maxScan);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> returnKey(final boolean returnKey) {
        findOptions.returnKey(returnKey);
        return this;
    }

    @Override
    public MorphiaKeyIterable<T> showRecordId(final boolean showRecordId) {
        findOptions.showRecordId(showRecordId);
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MorphiaKeyIterable<T> snapshot(final boolean snapshot) {
        findOptions.snapshot(snapshot);
        return this;
    }

    @Override
    public MongoCursor<Key<T>> iterator() {
        return new MongoBatchCursorAdapter<Key<T>>(execute());
    }

    public ReadOperation<BatchCursor<Key<T>>> asReadOperation() {
        throw new UnsupportedOperationException("operation not supported");
    }
}
