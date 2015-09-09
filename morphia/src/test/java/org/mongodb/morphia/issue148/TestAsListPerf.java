package org.mongodb.morphia.issue148;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


@Ignore("enable when testing performance issues")
public class TestAsListPerf extends TestBase {
    private static final Logger LOG = MorphiaLoggerFactory.get(TestAsListPerf.class);


    private final int nbOfAddresses = 500;
    private final int nbOfTasks = 200;
    private final int threadPool = 10;

    @Test
    public void compareDriverAndMorphiaQueryingMultithreaded() throws InterruptedException {
        final Result mongoQueryThreadsResult = new Result(nbOfTasks);
        final List<MongoQueryThread> mongoThreads = new ArrayList<MongoQueryThread>(nbOfTasks);
        for (int i = 0; i < nbOfTasks; i++) {
            mongoThreads.add(new MongoQueryThread(mongoQueryThreadsResult, nbOfAddresses));
        }
        final ExecutorService mongoPool = Executors.newFixedThreadPool(threadPool);
        for (final MongoQueryThread mongoQueryThread : mongoThreads) {
            mongoPool.execute(mongoQueryThread);
        }

        mongoPool.shutdown();
        mongoPool.awaitTermination(30, TimeUnit.SECONDS);

        final Result morphiaQueryThreadsResult = new Result(nbOfTasks);
        final List<MorphiaQueryThread> morphiaThreads = new ArrayList<MorphiaQueryThread>(nbOfTasks);
        for (int i = 0; i < nbOfTasks; i++) {
            morphiaThreads.add(new MorphiaQueryThread(morphiaQueryThreadsResult, nbOfAddresses));
        }
        final ExecutorService morphiaPool = Executors.newFixedThreadPool(threadPool);
        for (final MorphiaQueryThread thread : morphiaThreads) {
            morphiaPool.execute(thread);
        }
        morphiaPool.shutdown();
        morphiaPool.awaitTermination(30, TimeUnit.SECONDS);
        LOG.debug(format("compareDriverAndMorphiaQueryingMultithreaded (%d queries each) - driver: %4.2f ms/pojo (avg), "
                         + "morphia %4.2f ms/pojo (avg)", mongoQueryThreadsResult.results.size(), mongoQueryThreadsResult.getAverageTime(),
                         morphiaQueryThreadsResult.getAverageTime()));
    }

    @Test
    public void compareDriverAndMorphiaQueryingOnce() throws Exception {
        final double driverAvg = driverQueryAndMorphiaConverter(nbOfAddresses);
        final double morphiaAvg = morphiaQueryAndMorphiaConverter(nbOfAddresses);
        LOG.debug(format("compareDriverAndMorphiaQueryingOnce - driver: %4.2f ms/pojo , morphia: %4.2f ms/pojo ", driverAvg,
                         morphiaAvg));
        Assert.assertNotNull(driverAvg);
    }

    @Test
    public void compareMorphiaAndDriverQueryingMultithreaded() throws InterruptedException {
        final Result morphiaQueryThreadsResult = new Result(nbOfTasks);
        final List<MorphiaQueryThread> morphiaThreads = new ArrayList<MorphiaQueryThread>(nbOfTasks);
        for (int i = 0; i < nbOfTasks; i++) {
            morphiaThreads.add(new MorphiaQueryThread(morphiaQueryThreadsResult, nbOfAddresses));
        }
        final ExecutorService morphiaPool = Executors.newFixedThreadPool(threadPool);
        for (final MorphiaQueryThread thread : morphiaThreads) {
            morphiaPool.execute(thread);
        }

        morphiaPool.shutdown();
        morphiaPool.awaitTermination(30, TimeUnit.SECONDS);

        final Result mongoQueryThreadsResult = new Result(nbOfTasks);
        final List<MongoQueryThread> mongoThreads = new ArrayList<MongoQueryThread>(nbOfTasks);
        for (int i = 0; i < nbOfTasks; i++) {
            mongoThreads.add(new MongoQueryThread(mongoQueryThreadsResult, nbOfAddresses));
        }
        final ExecutorService mongoPool = Executors.newFixedThreadPool(threadPool);
        for (final MongoQueryThread mongoQueryThread : mongoThreads) {
            mongoPool.execute(mongoQueryThread);
        }

        mongoPool.shutdown();
        mongoPool.awaitTermination(30, TimeUnit.SECONDS);

        LOG.debug(format("compareMorphiaAndDriverQueryingMultithreaded (%d queries each) - driver: %4.2f ms/pojo (avg), "
                         + "morphia: %4.2f ms/pojo (avg)", mongoQueryThreadsResult.results.size(), mongoQueryThreadsResult.getAverageTime(),
                         morphiaQueryThreadsResult.getAverageTime()));
    }

    public double driverQueryAndMorphiaConverter(final int nbOfHits) {
        final long start = System.nanoTime();
        final List<DBObject> list = getDs().getDB().getCollection("Address")
                                           .find()
                                           .sort(new BasicDBObject("name", 1))
                                           .toArray();
        final EntityCache entityCache = new DefaultEntityCache();
        final List<Address> resultList = new LinkedList<Address>();
        for (final DBObject dbObject : list) {
            final Address address = getMorphia().fromDBObject(getDs(), Address.class, dbObject, entityCache);
            resultList.add(address);
        }
        final long duration = (System.nanoTime() - start) / 1000000; //ns -> ms
        Assert.assertEquals(nbOfHits, resultList.size());
        return (double) duration / nbOfHits;
    }

    @Test
    public void driverQueryingMultithreaded() throws InterruptedException {
        final Result mongoQueryThreadsResult = new Result(nbOfTasks);
        final List<MongoQueryThread> mongoThreads = new ArrayList<MongoQueryThread>(nbOfTasks);
        for (int i = 0; i < nbOfTasks; i++) {
            mongoThreads.add(new MongoQueryThread(mongoQueryThreadsResult, nbOfAddresses));
        }
        final ExecutorService mongoPool = Executors.newFixedThreadPool(threadPool);
        for (final MongoQueryThread mongoQueryThread : mongoThreads) {
            mongoPool.execute(mongoQueryThread);
        }

        mongoPool.shutdown();
        mongoPool.awaitTermination(30, TimeUnit.SECONDS);

        LOG.debug(format("driverQueryingMultithreaded - (%d queries) driver: %4.2f ms/pojo",
                         mongoQueryThreadsResult.results.size(), mongoQueryThreadsResult.getAverageTime()));

    }

    public double morphiaQueryAndMorphiaConverter(final int nbOfHits) {
        final Query<Address> query = getDs().createQuery(Address.class).
                                                                           order("name");
        final long start = System.nanoTime();
        final List<Address> resultList = query.asList();
        final long duration = (System.nanoTime() - start) / 1000000; //ns -> ms
        Assert.assertEquals(nbOfHits, resultList.size());
        return (double) duration / nbOfHits;
    }

    @Test
    public void morphiaQueryingMultithreaded() throws InterruptedException {
        final Result morphiaQueryThreadsResult = new Result(nbOfTasks);
        final List<MorphiaQueryThread> morphiaThreads = new ArrayList<MorphiaQueryThread>(nbOfTasks);
        for (int i = 0; i < nbOfTasks; i++) {
            morphiaThreads.add(new MorphiaQueryThread(morphiaQueryThreadsResult, nbOfAddresses));
        }
        final ExecutorService morphiaPool = Executors.newFixedThreadPool(threadPool);
        for (final MorphiaQueryThread thread : morphiaThreads) {
            morphiaPool.execute(thread);
        }
        morphiaPool.shutdown();
        morphiaPool.awaitTermination(30, TimeUnit.SECONDS);

        LOG.debug(format("morphiaQueryingMultithreaded - (%d queries) morphia: %4.2f ms/pojo",
                         morphiaQueryThreadsResult.results.size(), morphiaQueryThreadsResult.getAverageTime()));
    }

    @Override
    public void setUp() {
        super.setUp();
        getMorphia().map(Address.class);
        if (getDs().getCount(Address.class) == 0) {
            for (int i = 0; i < nbOfAddresses; i++) {
                final Address address = new Address(i);
                getDs().save(address);
            }
            getDs().find(Address.class).filter("name", "random").limit(-1).fetch();
        }
    }

    @Override
    public void cleanup() {
        //do nothing...
    }

    static class Result {

        private final Vector<Double> results;

        public Result(final int nbOfHits) {
            results = new Vector<Double>(nbOfHits);
        }

        public double getAverageTime() {
            Double total = 0d;
            for (final Double duration : results) {
                total += duration;
            }
            return total / results.size();
        }

    }

    @Entity
    private static class Address {

        @Id
        private ObjectId id;
        private int parity;
        private String name = "Scott";
        private String street = "3400 Maple";
        private String city = "Manhattan Beach";
        private String state = "CA";
        private int zip = 94114;
        private Date added = new Date();

        public Address() {

        }

        public Address(final int i) {
            parity = i % 2 == 0 ? 1 : 0;
            name += i;
            street += i;
            city += i;
            state += i;
            zip += i;
        }
    }

    class MorphiaQueryThread implements Runnable {
        private final Result result;
        private final int nbOfHits;

        public MorphiaQueryThread(final Result result, final int nbOfHits) {
            this.result = result;
            this.nbOfHits = nbOfHits;
        }

        @Override
        public void run() {
            result.results.add(morphiaQueryAndMorphiaConverter(nbOfHits));
        }
    }

    class MongoQueryThread implements Runnable {

        private final Result result;
        private final int nbOfHits;

        public MongoQueryThread(final Result result, final int nbOfHits) {
            this.result = result;
            this.nbOfHits = nbOfHits;
        }

        @Override
        public void run() {
            result.results.add(driverQueryAndMorphiaConverter(nbOfHits));
        }
    }
}
