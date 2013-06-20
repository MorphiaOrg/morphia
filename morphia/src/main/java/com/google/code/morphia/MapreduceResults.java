package com.google.code.morphia;


import java.util.Iterator;

import com.google.code.morphia.annotations.NotSaved;
import com.google.code.morphia.annotations.PreLoad;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.query.MorphiaIterator;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.mongodb.DBObject;


@SuppressWarnings({"unchecked", "rawtypes"})
@NotSaved
public class MapreduceResults<T> implements Iterable<T> {
  DBObject rawResults;
  private final Stats counts = new Stats();

  @Property("result")
  private String outColl;
  private long timeMillis;
  private boolean ok;
  private String err;
  private MapreduceType type;
  private QueryImpl baseQuery;
  //inline stuff
  @Transient
  private Class<T> clazz;
  @Transient
  private Mapper mapper;
  @Transient
  private EntityCache cache;

  public Stats getCounts() {
    return counts;
  }

  public long getElapsedMillis() {
    return timeMillis;
  }

  public boolean isOk() {
    return (ok);
  }

  public String getError() {
    return isOk() ? "" : err;
  }

  public MapreduceType getType() {
    return type;
  }

  public Query<T> createQuery() {
    return baseQuery.clone();
  }

  public void setInlineRequiredOptions(final Class<T> clazz, final Mapper mapper, final EntityCache cache) {
    this.clazz = clazz;
    this.mapper = mapper;
    this.cache = cache;
  }

  //Inline stuff
  public Iterator<T> getInlineResults() {
    return new MorphiaIterator<T, T>((Iterator<DBObject>) rawResults.get("results"), mapper, clazz, null, cache);
  }

  String getOutputCollectionName() {
    return outColl;
  }

  void setBits(final MapreduceType t, final QueryImpl baseQ) {
    type = t;
    baseQuery = baseQ;
  }

  @PreLoad
  void preLoad(final DBObject raw) {
    rawResults = raw;
  }

  public static class Stats {
    private int input;
    private int emit;
    private int output;

    public int getInputCount() {
      return input;
    }

    public int getEmitCount() {
      return emit;
    }

    public int getOutputCount() {
      return output;
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
