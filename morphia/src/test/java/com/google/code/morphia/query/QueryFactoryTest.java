package com.google.code.morphia.query;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.TestBase;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class QueryFactoryTest extends TestBase {

  @Test
  public void changeQueryFactory() {
    QueryFactory current = ds.getQueryFactory();
    QueryFactory custom = new DefaultQueryFactory();
    
    ds.setQueryFactory(custom);
    
    Assert.assertNotSame(current, ds.getQueryFactory());
    Assert.assertSame(custom, ds.getQueryFactory());
  }
  
  @Test
  public void createQuery() {
    
    final AtomicInteger counter = new AtomicInteger();
    
    QueryFactory queryFactory = new DefaultQueryFactory() {
      @Override
      public <T> Query<T> createQuery(Datastore datastore,
          DBCollection collection, Class<T> type, DBObject query) {
        
        counter.incrementAndGet();
        return super.createQuery(datastore, collection, type, query);
      }
    };
    
    ds.setQueryFactory(queryFactory);
    
    Query<String> query = ds.createQuery(String.class);
    Query<String> other = ds.createQuery(String.class);
    
    Assert.assertNotSame(other, query);
    Assert.assertEquals(2, counter.get());
  }
}
