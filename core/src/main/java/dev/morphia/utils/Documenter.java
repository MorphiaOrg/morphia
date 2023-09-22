package dev.morphia.utils;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.lang.NonNull;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.Operations;
import dev.morphia.query.internal.DatastoreAware;
import dev.morphia.query.updates.UpdateOperator;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static java.util.Arrays.asList;

public class Documenter {
    @NonNull
    public static <T> List<T> coalesce(T first, T[] updates) {
        List<T> operators = new ArrayList<>();
        operators.add(first);
        operators.addAll(asList(updates));
        return operators;
    }

    /**
     * Converts the updates to a Document
     *
     * @return the query
     * @morphia.internal
     */
    @MorphiaInternal
    public static Document toDocument(MorphiaDatastore datastore, EntityModel entityModel, List<UpdateOperator> updates,
            boolean validate) {
        final Operations operations = new Operations(datastore, entityModel);

        for (UpdateOperator update : updates) {
            PathTarget pathTarget = new PathTarget(datastore.getMapper(), entityModel, update.field(), validate);
            if (update instanceof DatastoreAware) {
                ((DatastoreAware) update).setDatastore(datastore);
            }
            operations.add(update.operator(), update.toTarget(pathTarget));
        }
        return operations.toDocument();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<Document> toDocument(MorphiaDatastore datastore, List<Stage> updates) {
        CodecRegistry registry = datastore.getCodecRegistry();
        List<Document> documents = new ArrayList<>();
        for (Stage update : updates) {
            DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());
            Codec codec = registry.get(update.getClass());
            codec.encode(writer, update, EncoderContext.builder().build());
            documents.add(writer.getDocument());
        }
        return documents;
    }
}
