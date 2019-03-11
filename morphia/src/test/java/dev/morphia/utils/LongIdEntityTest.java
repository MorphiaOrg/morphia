package dev.morphia.utils;


import org.junit.Test;
import dev.morphia.Datastore;
import dev.morphia.TestBase;

import static org.junit.Assert.assertEquals;


/**
 * @author ScottHernandez
 */
public class LongIdEntityTest extends TestBase {
    @Test
    public void testMonoIncreasingId() throws Exception {
        MyEntity ent = new MyEntity(getDs());
        getDs().save(ent);
        assertEquals(1L, ent.getMyLongId(), 0);
        ent = new MyEntity(getDs());
        getDs().save(ent);
        assertEquals(2L, ent.getMyLongId(), 0);
    }

    static class MyEntity extends LongIdEntity {
        protected MyEntity() {
            super(null);
        }

        MyEntity(final Datastore ds) {
            super(ds);
        }
    }

}
