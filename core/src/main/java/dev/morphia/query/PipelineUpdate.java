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
public class PipelineUpdate<T> extends UpdateBase<T, Stage> {
    PipelineUpdate(Datastore datastore, MongoCollection<T> collection, Query<T> query, Class<T> type, List<Stage> updates) {
        super(datastore, collection, query, type, updates);
    }

    /**
     * Executes the update
     *
     * @param options the options to apply
     * @return the results
     */
    public UpdateResult execute(UpdateOptions options) {
        List<Document> updateOperations = toDocument();
        final Document queryObject = getQuery().toDocument();

        ClientSession session = getDatastore().findSession(options);
        MongoCollection<T> mongoCollection = options.prepare(getCollection());
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
        CodecRegistry registry = getDatastore().getCodecRegistry();
        List<Document> documents = new ArrayList<>();
        for (Stage update : getUpdates()) {
            DocumentWriter writer = new DocumentWriter(getMapper());
            Codec codec = registry.get(update.getClass());
            codec.encode(writer, update, EncoderContext.builder().build());
            documents.add(writer.getDocument());
        }
        return documents;
    }
}
