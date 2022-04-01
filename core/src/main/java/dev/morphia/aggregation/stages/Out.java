package dev.morphia.aggregation.stages;

import com.mongodb.lang.Nullable;
import org.bson.Document;

/**
 * Takes the documents returned by the aggregation pipeline and writes them to a specified collection. The $out operator must be the last
 * stage in the pipeline. The $out operator lets the aggregation framework return result sets of any size.
 *
 * @param <O> the output type used to lookup the collection name
 * @aggregation.expression $out
 */
public class Out<O> extends Stage {
    private Class<?> type;
    private String collection;

    protected Out() {
        super("$out");
    }

    /**
     * Creates a $out stage with target type/collection
     *
     * @param type the type to use to determine the target collection
     * @param <O>  the output type used to lookup the collection name
     * @return the new stage
     */
    public static <O> Out<O> to(Class<O> type) {
        return new Out<O>()
                   .type(type);
    }

    /**
     * Creates a $out stage with target collection
     *
     * @param collection the target collection
     * @return the new stage
     */
    public static Out<Document> to(String collection) {
        return new Out<Document>()
                   .collection(collection);
    }

    /**
     * @return the collection name
     * @morphia.internal
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @return the type representing the collection
     * @morphia.internal
     */
    @Nullable
    public Class<?> getType() {
        return type;
    }

    private Out<O> collection(String collection) {
        this.collection = collection;
        return this;
    }

    private Out<O> type(Class<O> type) {
        this.type = type;
        return this;
    }
}
