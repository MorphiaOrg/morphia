package org.mongodb.morphia;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.annotations.CappedAt;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCapped extends TestBase {
    @Test
    public void testCappedEntity() throws Exception {
        // given
        getMorphia().map(CurrentStatus.class);
        getDs().ensureCaps();

        // when-then
        final CurrentStatus cs = new CurrentStatus("All Good");
        getDs().save(cs);
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        getDs().save(new CurrentStatus("Kinda Bad"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        assertTrue(getDs().find(CurrentStatus.class).limit(1).get().message.contains("Bad"));
        getDs().save(new CurrentStatus("Kinda Bad2"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        getDs().save(new CurrentStatus("Kinda Bad3"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
        getDs().save(new CurrentStatus("Kinda Bad4"));
        assertEquals(1, getDs().getCount(CurrentStatus.class));
    }

    @Entity(cap = @CappedAt(count = 1))
    private static class CurrentStatus {
        @Id
        private ObjectId id;
        private String message;

        private CurrentStatus() {
        }

        public CurrentStatus(final String msg) {
            message = msg;
        }
    }

}
