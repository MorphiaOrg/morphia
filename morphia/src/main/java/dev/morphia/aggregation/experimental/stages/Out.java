package dev.morphia.aggregation.experimental.stages;

import org.bson.Document;

/**
 * Takes the documents returned by the aggregation pipeline and writes them to a specified collection. The $out operator must be the last
 * stage in the pipeline. The $out operator lets the aggregation framework return result sets of any size.
 *
 * @param <O> the output type used to lookup the collection name
 * @mongodb.driver.manual reference/operator/aggregation/out/ $out
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
    public static <O> Out<O> to(final Class<O> type) {
        return new Out<O>()
                   .type(type);
    }

    private Out<O> type(final Class<O> type) {
        this.type = type;
        return this;
    }

    /**
     * Creates a $out stage with target collection
     *
     * @param collection the target collection
     * @return the new stage
     */
    public static Out<Document> to(final String collection) {
        return new Out<Document>()
                   .collection(collection);
    }

    private Out<O> collection(final String collection) {
        this.collection = collection;
        return this;
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
    public Class<?> getType() {
        return type;
    }
}
