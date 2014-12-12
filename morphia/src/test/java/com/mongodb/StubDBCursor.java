package com.mongodb;

import java.util.concurrent.TimeUnit;

/**
 * Somewhat ugly, but effective, way to stub out the collection.  Can be supplied with a StubDBCollection. Can override the cursor 
 * methods to check what's actually being called.
 *
 * This is currently being used to unit test QueryImpl.
 */
public class StubDBCursor extends DBCursor {
    private long maxTime;
    private TimeUnit maxTimeUnit;

    public StubDBCursor(final DBCollection collection) {
        super(collection, new BasicDBObject(), new BasicDBObject(), ReadPreference.primary());
    }

    @Override
    public DBCursor maxTime(final long maxTime, final TimeUnit timeUnit) {
        this.maxTime = maxTime;
        this.maxTimeUnit = timeUnit;
        return this;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public TimeUnit getMaxTimeUnit() {
        return maxTimeUnit;
    }
}
