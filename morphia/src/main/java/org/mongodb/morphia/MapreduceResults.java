package org.mongodb.morphia;


import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.query.Query;

import java.util.Iterator;

/**
 * Stores the results of a map reduce operation
 *
 * @param <T> the type of the results
 */
@NotSaved
@SuppressWarnings("deprecation")
public class MapreduceResults<T> implements Iterable<T> {
    private static final Logger LOG = MorphiaLoggerFactory.get(MapreduceResults.class);
    private final Stats counts = new Stats();
    private MapReduceOutput output;
    private String outputCollectionName;
    private OutputType outputType;
    private Query<T> query;

    @Transient
    private Class<T> clazz;
    @Transient
    private Mapper mapper;
    @Transient
    private EntityCache cache;
    private Datastore datastore;

    /**
     * Creates a results instance for the given output
     *
     * @param output the output of the operation
     */
    public MapreduceResults(final MapReduceOutput output) {
        this.output = output;
        outputCollectionName = output.getCollectionName();
    }

    /**
     * @return the query to use against these results
     */
    public Query<T> createQuery() {
        if (outputType == OutputType.INLINE) {
            throw new MappingException("No collection available for inline mapreduce jobs");
        }
        return query.cloneQuery();
    }

    /**
     * @return the Stats for the operation
     */
    public Stats getCounts() {
        return counts;
    }

    /**
     * @return the duration of the operation
     */
    public long getElapsedMillis() {
        return output.getDuration();
    }

    /**
     * @return will always return null
     */
    @Deprecated
    public String getError() {
        LOG.warning("MapreduceResults.getError() will always return null.");
        return null;
    }

    /**
     * Creates an Iterator over the results of the operation.  This method should probably not be called directly as it requires more
     * context to use properly.  Using {@link #iterator()} will return the proper Iterator regardless of the type of map reduce operation
     * performed.
     *
     * @return the Iterator
     * @see MapreduceType
     */
    public Iterator<T> getInlineResults() {
        return datastore.getIteratorFactory().createIterator(datastore, output.results().iterator(), mapper, clazz, null, cache);
    }

    /**
     * @return the type of the operation
     * @deprecated use {@link #getOutputType()} instead
     */
    @Deprecated
    public MapreduceType getType() {
        if (outputType == OutputType.REDUCE) {
            return MapreduceType.REDUCE;
        } else if (outputType == OutputType.MERGE) {
            return MapreduceType.MERGE;
        } else if (outputType == OutputType.INLINE) {
            return MapreduceType.INLINE;
        } else {
            return MapreduceType.REPLACE;
        }

    }

    @Deprecated
    void setType(final MapreduceType type) {
        this.outputType = type.toOutputType();
    }

    /**
     * @return the type of the operation
     * @since 1.3
     */
    public OutputType getOutputType() {
        return outputType;
    }

    /**
     * Sets the output type for this mapreduce job
     *
     * @param outputType the output type
     * @since 1.3
     */
    public void setOutputType(final OutputType outputType) {
        this.outputType = outputType;
    }

    /**
     * @return will always return true
     */
    @Deprecated
    public boolean isOk() {
        LOG.warning("MapreduceResults.isOk() will always return true.");
        return true;
    }

    /**
     * Creates an Iterator over the results of the operation.
     *
     * @return the Iterator
     */
    @Override
    public Iterator<T> iterator() {
        return outputType == OutputType.INLINE ? getInlineResults() : createQuery().fetch().iterator();
    }

    /**
     * Sets the required options when the operation type was INLINE
     *
     * @param datastore the Datastore to use when fetching this reference
     * @param clazz     the type of the results
     * @param mapper    the mapper to use
     * @param cache     the cache of entities seen so far
     * @see OutputType
     */
    public void setInlineRequiredOptions(final Datastore datastore, final Class<T> clazz, final Mapper mapper, final EntityCache cache) {
        this.mapper = mapper;
        this.datastore = datastore;
        this.clazz = clazz;
        this.cache = cache;
    }

    /**
     * This class represents various statistics about a map reduce operation
     */
    public class Stats {
        /**
         * @return the emit count of the operation
         */
        public int getEmitCount() {
            return output.getEmitCount();
        }

        /**
         * @return the input count of the operation
         */
        public int getInputCount() {
            return output.getInputCount();
        }

        /**
         * @return the output count of the operation
         */
        public int getOutputCount() {
            return output.getOutputCount();
        }
    }

    String getOutputCollectionName() {
        return outputCollectionName;
    }

    void setQuery(final Query<T> query) {
        this.query = query;
    }
}
