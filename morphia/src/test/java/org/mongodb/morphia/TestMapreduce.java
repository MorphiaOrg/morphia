package org.mongodb.morphia;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PreLoad;
import org.mongodb.morphia.testmodel.Circle;
import org.mongodb.morphia.testmodel.Rectangle;
import org.mongodb.morphia.testmodel.Shape;

import java.util.Iterator;
import java.util.Random;


public class TestMapreduce extends TestBase {

    @Test(expected = MongoException.class)
    public void testBadMR() throws Exception {
        final String map = "function () { if(this['radius']) { doEmit('circle', {count:1}); return; } emit('rect', {count:1}); }";
        final String reduce = "function (key, values) { var total = 0; for ( var i=0; i<values.length; i++ ) {total += values[i].count;} "
                              + "return { count : total }; }";

        getDs().mapReduce(MapreduceType.REPLACE, getAds().createQuery(Shape.class), map, reduce, null, null, ResultEntity.class);
    }

    @Test
    public void testMR() throws Exception {
        final Random rnd = new Random();

        //create 100 circles and rectangles
        for (int i = 0; i < 100; i++) {
            getAds().insert("shapes", new Circle(rnd.nextDouble()));
            getAds().insert("shapes", new Rectangle(rnd.nextDouble(), rnd.nextDouble()));
        }
        final String map = "function () { if(this['radius']) { emit('circle', {count:1}); return; } emit('rect', {count:1}); }";
        final String reduce = "function (key, values) { var total = 0; for ( var i=0; i<values.length; i++ ) {total += values[i].count;} "
                              + "return { count : total }; }";

        final MapreduceResults<ResultEntity> mrRes =
            getDs().mapReduce(MapreduceType.REPLACE, getAds().createQuery(Shape.class), map, reduce, null, null, ResultEntity.class);
        Assert.assertEquals(2, mrRes.createQuery().countAll());
        Assert.assertEquals(100, mrRes.createQuery().get().getValue().count, 0);


        final MapreduceResults<ResultEntity> inline =
            getDs().mapReduce(MapreduceType.INLINE, getAds().createQuery(Shape.class), map, reduce, null, null, ResultEntity.class);
        final Iterator<ResultEntity> iterator = inline.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        Assert.assertEquals(2, count);
        Assert.assertEquals(100, inline.iterator().next().getValue().count, 0);
    }

    @Entity("mr_results")
    private static class ResultEntity extends ResultBase<String, HasCount> {
    }

    public static class ResultBase<T, V> {
        @Id
        private T type;
        @Embedded
        private V value;

        public T getType() {
            return type;
        }

        public void setType(final T type) {
            this.type = type;
        }

        public V getValue() {
            return value;
        }

        public void setValue(final V value) {
            this.value = value;
        }
    }

    private static class HasCount {
        private double count;
    }

    @Entity("mr-results")
    private static class ResultEntity2 {
        @Id
        private String type;
        private double count;

        @PreLoad
        void preLoad(final BasicDBObject dbObj) {
            //pull all the fields from value field into the parent.
            dbObj.putAll((DBObject) dbObj.get("value"));
        }
    }

}
