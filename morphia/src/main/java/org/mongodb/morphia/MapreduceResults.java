package org.mongodb.morphia;


import com.mongodb.MapReduceOutput;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;

import java.util.Iterator;


@NotSaved
public class MapreduceResults<T> implements Iterable<T> {
    private static final Logger LOG = MorphiaLoggerFactory.get(MapreduceResults.class);

    private MapReduceOutput mpo;
    private final Stats counts = new Stats();

    private String outColl;
    private MapreduceType type;
    private Query<T> query;

    @Transient
    private Class<T> clazz;
    @Transient
    private Mapper mapper;
    @Transient
    private EntityCache cache;

    public MapreduceResults(final MapReduceOutput mpo) {
        this.mpo = mpo;
        outColl = mpo.getCollectionName();
    }

    public Stats getCounts() {
        return counts;
    }

    public long getElapsedMillis() {
        return mpo.getDuration();
    }

    @Deprecated
    public boolean isOk() {
        LOG.warning("MapreduceResults.isOk() will always return true.");
        return true;
    }

    public String getError() {
        LOG.warning("MapreduceResults.getError() will always return null.");
        return null;
    }
    
    public MapreduceType getType() {
        return type;
    }

    public Query<T> createQuery() {
        if (type == MapreduceType.INLINE) {
            throw new MappingException("No collection available for inline mapreduce jobs");
        }
        return query.cloneQuery();
    }

    public void setInlineRequiredOptions(final Class<T> clazz, final Mapper mapper, final EntityCache cache) {
        this.clazz = clazz;
        this.mapper = mapper;
        this.cache = cache;
    }

    //Inline stuff
    public Iterator<T> getInlineResults() {
        return new MorphiaIterator<T, T>(mpo.results().iterator(), mapper, clazz, null, cache);
    }

    String getOutputCollectionName() {
        return outColl;
    }

    void setType(final MapreduceType type) {
        this.type = type;
    }

    void setQuery(final Query<T> query) {
        this.query = query;
    }

    public class Stats {
        public int getInputCount() {
            return mpo.getInputCount();
        }

        public int getEmitCount() {
            return mpo.getEmitCount();
        }

        public int getOutputCount() {
            return mpo.getOutputCount();
        }
    }

    public Iterator<T> iterator() {
        if (type == MapreduceType.INLINE) {
            return getInlineResults();
        } else {
            return createQuery().fetch().iterator();
        }
    }
}
