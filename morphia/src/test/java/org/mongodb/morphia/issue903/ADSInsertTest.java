package org.mongodb.morphia.issue903;

import java.util.ArrayList;

import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.testutil.TestEntity;

import com.mongodb.WriteConcern;

public class ADSInsertTest extends TestBase {
    @Test
    public void testADSBulkInsertEmptyIterable() {
        this.getAds().insert("class_1_collection", new ArrayList<Class1>());
    }

    @Test
    public void testADSBulkInsertWithoutCollection() {
        this.getAds().insert(new ArrayList<Class1>(), WriteConcern.NORMAL);
    }

    @Test
    public void testADSBulkInsertWithNullWC() {
        this.getAds().insert(new ArrayList<Class1>(), null);
    }

    @Test
    public void testADSBulkInsertEmptyVararg() {
        this.getAds().insert();
    }

    @Entity
    private static class Class1 extends TestEntity {
    }
}
