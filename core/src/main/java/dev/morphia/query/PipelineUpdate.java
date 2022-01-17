package dev.morphia.query;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.experimental.stages.Stage;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines an update operation
 *
 * @param <T>
 */
public class PipelineUpdate<T> {
    private final Query<T> query;
    private final MongoCollection<T> collection;
    private final Class<T> type;
    private final List<Stage> updates = new ArrayList<>();
    private final Datastore datastore;

    PipelineUpdate(Datastore datastore, MongoCollection<T> collection, Query<T> query, Class<T> type, List<Stage> updates) {
        this.datastore = datastore;
        this.collection = collection;
        this.query = query;
        this.type = type;
        this.updates.addAll(updates);
    }

    /**
     * Executes the update
     *
     * @return the results
     */
    public UpdateResult execute() {
        return execute(new UpdateOptions());
    }

    /**
     * Executes the update
     *
     * @param options the options to apply
     * @return the results
     */
    public UpdateResult execute(UpdateOptions options) {
        List<Document> updateOperations = toDocument();
        final Document queryObject = query.toDocument();

        ClientSession session = datastore.findSession(options);
        MongoCollection<T> mongoCollection = options.prepare(collection);
        if (options.isMulti()) {
            return session == null ? mongoCollection.updateMany(queryObject, updateOperations, options)
                                   : mongoCollection.updateMany(session, queryObject, updateOperations, options);
        } else {
            return session == null ? mongoCollection.updateOne(queryObject, updateOperations, options)
                                   : mongoCollection.updateOne(session, queryObject, updateOperations, options);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Document> toDocument() {
        CodecRegistry registry = datastore.getCodecRegistry();
        List<Document> documents = new ArrayList<>();
        for (Stage update : updates) {
            DocumentWriter writer = new DocumentWriter(datastore.getMapper());
            Codec codec = registry.get(update.getClass());
            codec.encode(writer, update, EncoderContext.builder().build());
            documents.add(writer.getDocument());
        }
        return documents;
    }
}
