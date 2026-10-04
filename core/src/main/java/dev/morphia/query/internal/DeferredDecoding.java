package dev.morphia.query.internal;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.transactions.SessionDatastore;

import org.bson.RawBsonDocument;
import org.bson.codecs.Decoder;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Decodes query results after the driver hands them over rather than while the driver is still executing the command.
 * <p>
 * Decoding an entity can issue further queries, e.g. to resolve an eager {@code @Reference}. Inside a session those nested
 * queries run on the same {@code ClientSession} while the outer command is still open, which drivers 5.12+ reject. Within a
 * session, results are therefore fetched as {@link RawBsonDocument}s and decoded here, once the driver has returned them.
 *
 * @hidden
 * @morphia.internal
 */
@MorphiaInternal
public final class DeferredDecoding {
    private DeferredDecoding() {
    }

    /**
     * @param datastore the datastore running the operation
     * @return true if results should be fetched raw and decoded after the driver returns them
     */
    public static boolean required(MorphiaDatastore datastore) {
        return datastore instanceof SessionDatastore;
    }

    /**
     * @param collection the collection whose document class and codecs would otherwise decode the results
     * @param <T>        the document type
     * @return the same collection, returning raw documents
     */
    public static <T> MongoCollection<RawBsonDocument> raw(MongoCollection<T> collection) {
        return collection.withDocumentClass(RawBsonDocument.class);
    }

    /**
     * @param registry the codec registry to use
     * @param type     the type to decode to
     * @param raw      the raw document, possibly null
     * @param <T>      the decoded type
     * @return the decoded value, or null if {@code raw} was null
     */
    @Nullable
    public static <T> T decode(CodecRegistry registry, Class<T> type, @Nullable RawBsonDocument raw) {
        return raw == null ? null : raw.decode(registry.get(type));
    }

    /**
     * @param cursor   the raw cursor
     * @param registry the codec registry to use
     * @param type     the type to decode to
     * @param <T>      the decoded type
     * @return a cursor decoding each document as it is returned
     */
    public static <T> MongoCursor<T> cursor(MongoCursor<RawBsonDocument> cursor, CodecRegistry registry, Class<T> type) {
        return new DecodingCursor<>(cursor, registry.get(type));
    }

    private static final class DecodingCursor<T> implements MongoCursor<T> {
        private final MongoCursor<RawBsonDocument> wrapped;
        private final Decoder<T> decoder;

        private DecodingCursor(MongoCursor<RawBsonDocument> wrapped, Decoder<T> decoder) {
            this.wrapped = wrapped;
            this.decoder = decoder;
        }

        @Override
        public void close() {
            wrapped.close();
        }

        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        @Override
        public T next() {
            return wrapped.next().decode(decoder);
        }

        @Override
        public int available() {
            return wrapped.available();
        }

        @Nullable
        @Override
        public T tryNext() {
            RawBsonDocument next = wrapped.tryNext();
            return next == null ? null : next.decode(decoder);
        }

        @Nullable
        @Override
        public ServerCursor getServerCursor() {
            return wrapped.getServerCursor();
        }

        @Override
        public ServerAddress getServerAddress() {
            return wrapped.getServerAddress();
        }
    }
}
