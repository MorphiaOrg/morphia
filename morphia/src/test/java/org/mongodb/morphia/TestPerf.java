/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.mongodb.morphia;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ReflectionDBObject;
import com.mongodb.WriteConcern;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.logging.Logger;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.query.MorphiaIterator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author Scott Hernandez
 */
@Ignore("This seems an arbitrary boundary to check against and fails intermittently on jenkins")
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestPerf extends TestBase {
    private static final Logger LOG = MorphiaLoggerFactory.get(TestPerf.class);

    private static final double WRITE_FAIL_FACTOR = 1.75;
    private static final double READ_FAIL_FACTOR = 1.75;
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    @Entity
    public static class Address {
        @Id
        private ObjectId id;
        private String name = "Scott";
        private String street = "3400 Maple";
        private String city = "Manhattan Beach";
        private String state = "CA";
        private int zip = 90266;
        private Date added = new Date();

        public Address() {
        }

        public Address(final BasicDBObject dbObj) {
            name = dbObj.getString("name");
            street = dbObj.getString("street");
            city = dbObj.getString("city");
            state = dbObj.getString("state");
            zip = dbObj.getInt("zip");
            added = (Date) dbObj.get("added");
        }

        public DBObject toDBObject() {
            final DBObject dbObj = new BasicDBObject();
            dbObj.put("name", name);
            dbObj.put("street", street);
            dbObj.put("city", city);
            dbObj.put("state", state);
            dbObj.put("zip", zip);
            dbObj.put("added", new Date());
            return dbObj;
        }
    }

    public static class Address2 extends ReflectionDBObject {
        private String name = "Scott";
        private String street = "3400 Maple";
        private String city = "Manhattan Beach";
        private String state = "CA";
        private int zip = 90266;
        private Date added = new Date();

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(final String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(final String state) {
            this.state = state;
        }

        public int getZip() {
            return zip;
        }

        public void setZip(final int zip) {
            this.zip = zip;
        }

        public Date getAdded() {
            return added;
        }

        public void setAdded(final Date added) {
            this.added = added;
        }

    }

    @Test
    public void testAddressInsertPerformance() throws Exception {
        final int count = 10000;
        final boolean strict = true;
        long startTicks = new Date().getTime();
        insertAddresses(count, true, strict);
        long endTicks = new Date().getTime();
        final long rawInsertTime = endTicks - startTicks;

        getDs().delete(getDs().find(Address.class));
        startTicks = new Date().getTime();
        insertAddresses(count, false, strict);
        endTicks = new Date().getTime();
        final long insertTime = endTicks - startTicks;

        final String msg = String.format("Insert (%s) performance is too slow: %sX slower (%s/%s)",
                                         count, DF.format((double) insertTime / rawInsertTime), insertTime, rawInsertTime);
        Assert.assertTrue(msg, insertTime < (rawInsertTime * WRITE_FAIL_FACTOR));
    }

    @Test
    public void testAddressLoadPerformance() throws Exception {
        insertAddresses(5001, true, false);

        final int count = 5000;
        long startTicks = new Date().getTime();
        loadAddresses2(count, true);
        long endTicks = new Date().getTime();
        final long rawLoadTime = endTicks - startTicks;

        startTicks = new Date().getTime();
        loadAddresses2(count, false);
        endTicks = new Date().getTime();
        final long morphiaLoadTime = endTicks - startTicks;

        startTicks = new Date().getTime();
        loadAddresses3(count, true);
        endTicks = new Date().getTime();
        final long reflectLoadTime = endTicks - startTicks;

        String msg = String.format("Load (%s) performance is too slow compared to ReflectionDBObject: %sX slower (%s/%s)",
                                   count, DF.format((double) morphiaLoadTime / reflectLoadTime), morphiaLoadTime, reflectLoadTime);
        Assert.assertTrue(msg, morphiaLoadTime < (reflectLoadTime * READ_FAIL_FACTOR));

        msg = String.format("Load (%s) performance is too slow compared to raw: %sX slower (%s/%s)",
                            count, DF.format((double) morphiaLoadTime / rawLoadTime), morphiaLoadTime, rawLoadTime);
        Assert.assertTrue(msg, morphiaLoadTime < (rawLoadTime * READ_FAIL_FACTOR));

    }

    public void loadAddresses(final int count, final boolean raw) {
        final DBCollection dbColl = getDb().getCollection(((DatastoreImpl) getDs()).getMapper().getCollectionName(Address.class));

        for (int i = 0; i < count; i++) {
            if (raw) {
                new Address((BasicDBObject) dbColl.findOne());
            } else {
                getDs().find(Address.class).get();
            }
        }
    }

    public void loadAddresses2(final int count, final boolean raw) {
        final DBCollection dbColl = getDb().getCollection(((DatastoreImpl) getDs()).getMapper().getCollectionName(Address.class));
        final Iterable it = raw ? dbColl.find().limit(count) : getDs().find(Address.class).limit(count).fetch();

        for (final Object o : it) {
            if (raw) {
                new Address((BasicDBObject) o);
            }
        }
        if (!raw) {
            LOG.info("driverTime: " + ((MorphiaIterator<Object, Object>) it).getDriverTime() + "ms, mapperTime:"
                                  + ((MorphiaIterator<Object, Object>) it).getMapperTime() + "ms");
        }
    }

    public void loadAddresses3(final int count, final boolean raw) {
        final DBCollection dbColl = getDb().getCollection(((DatastoreImpl) getDs()).getMapper().getCollectionName(Address.class));
        final Iterable it = raw ? dbColl.find().limit(count) : getDs().find(Address.class).limit(count).fetch();

        if (raw) {
            dbColl.setObjectClass(Address2.class);
        }

        for (final Object o : it) {
            if (raw) {
                // noop ?
            } else {
                //no-op; already happened during iteration
            }
        }


        if (!raw) {
            LOG.info("driverTime: " + ((MorphiaIterator<Object, Object>) it).getDriverTime() + "ms, mapperTime:"
                               + ((MorphiaIterator<Object, Object>) it).getMapperTime() + "ms");
        }
    }

    public void insertAddresses(final int count, final boolean raw, final boolean strict) {
        final DBCollection dbColl = getDb().getCollection(((DatastoreImpl) getDs()).getMapper().getCollectionName(Address.class));

        for (int i = 0; i < count; i++) {
            final Address address = new Address();
            if (raw) {
                final DBObject dbObj = address.toDBObject();
                if (strict) {
                    dbColl.save(dbObj, WriteConcern.SAFE);
                } else {
                    dbColl.save(dbObj, WriteConcern.NORMAL);
                }
            } else {
                if (strict) {
                    getDs().save(address, WriteConcern.SAFE);
                } else {
                    getDs().save(address, WriteConcern.NORMAL);
                }
            }
        }
    }

    @Entity(value = "imageMeta", noClassnameStored = true)
    public static class TestObj {
        @Id
        private ObjectId id = new ObjectId();
        private long var1;
        private long var2;

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public long getVar1() {
            return var1;
        }

        public void setVar1(final long var1) {
            this.var1 = var1;
        }

        public long getVar2() {
            return var2;
        }

        public void setVar2(final long var2) {
            this.var2 = var2;
        }
    }

    @Test
    public void testDifference() {
        try {
            final Morphia morphia = new Morphia();
            morphia.map(TestObj.class);
            //create the list
            List<TestObj> objList = new ArrayList<TestObj>();
            for (int i = 0; i < 1000; i++) {
                final TestObj obj = new TestObj();
                obj.id = new ObjectId();
                obj.var1 = 3345345L + i;
                obj.var2 = 6785678L + i;
                objList.add(obj);
            }

            long start = System.currentTimeMillis();
            for (final TestObj to : objList) {
                getAds().insert(to, WriteConcern.SAFE);
            }
            LOG.info("Time taken morphia: " + (System.currentTimeMillis() - start) + "ms");

            final MongoClient mongoConn = new MongoClient("localhost", 27017);
            final DB mongoDB = mongoConn.getDB("my_database");
            List<DBObject> batchPush = new ArrayList<DBObject>();
            for (int i = 0; i < 1000; i++) {
                final DBObject doc = new BasicDBObject();
                doc.put("_id", new ObjectId());
                doc.put("var1", 3345345L + i);
                doc.put("var2", 6785678L + i);
                batchPush.add(doc);
            }
            final DBCollection c = mongoDB.getCollection("imageMeta2");
            c.setWriteConcern(WriteConcern.SAFE);
            start = System.currentTimeMillis();
            for (final DBObject doc : batchPush) {
                c.insert(doc);
            }
            LOG.info("Time taken regular: " + (System.currentTimeMillis() - start) + "ms");

            objList = new ArrayList<TestObj>();
            for (int i = 0; i < 1000; i++) {
                final TestObj obj = new TestObj();
                obj.id = new ObjectId();
                obj.var1 = 3345345L + i;
                obj.var2 = 6785678L + i;
                objList.add(obj);
            }

            start = System.currentTimeMillis();
            getAds().insert(objList, WriteConcern.SAFE);
            LOG.info("Time taken batch morphia: " + (System.currentTimeMillis() - start) + "ms");


            batchPush = new ArrayList<DBObject>();
            for (int i = 0; i < 1000; i++) {
                final DBObject doc = new BasicDBObject();
                doc.put("_id", new ObjectId());
                doc.put("var1", 3345345L + i);
                doc.put("var2", 6785678L + i);
                batchPush.add(doc);
            }

            start = System.currentTimeMillis();
            c.insert(batchPush);
            LOG.info("Time taken batch regular: " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

    }
}
