package dev.morphia.query;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.DatastoreImpl;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.stages.Stage;
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
class PipelineUpdate<T> {
    private final Query<T> query;
    private final MongoCollection<T> collection;
    private final List<Stage> updates = new ArrayList<>();
    private final DatastoreImpl datastore;

    PipelineUpdate(DatastoreImpl datastore, MongoCollection<T> collection, Query<T> query, List<Stage> updates) {
        this.datastore = datastore;
        this.collection = collection;
        this.query = query;
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

        MongoCollection<T> mongoCollection = datastore.configureCollection(options, collection);
        if (options.multi()) {
            return datastore.operations().updateMany(mongoCollection, queryObject, updateOperations, options);
        } else {
            return datastore.operations().updateOne(mongoCollection, queryObject, updateOperations, options);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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
