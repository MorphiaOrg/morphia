package com.mongodb;

import java.util.List;

/**
 * Somewhat ugly, but effective, way to stub out the collection.  Still needs a real DB, otherwise you get null pointers, 
 * but it does allow you to override with your own behaviours.  
 * 
 * This is currently being used to unit test QueryImpl.
 */
public class StubDBCollection extends DBCollection {

    private StubDBCursor stubDBCursor;

    public StubDBCollection(final DB db) {
        super(db, "");
    }

    @Override
    public WriteResult insert(final List<DBObject> list, final WriteConcern concern, final DBEncoder encoder) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public WriteResult update(final DBObject q, final DBObject o, final boolean upsert, final boolean multi,
                              final WriteConcern concern, final DBEncoder encoder) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    protected void doapply(final DBObject o) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public WriteResult remove(final DBObject o, final WriteConcern concern, final DBEncoder encoder) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    QueryResultIterator find(final DBObject ref, final DBObject fields, final int numToSkip, final int batchSize, final int limit,
                             final int options, final ReadPreference readPref,
                             final DBDecoder decoder) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    QueryResultIterator find(final DBObject ref, final DBObject fields, final int numToSkip, final int batchSize, final int limit,
                             final int options, final ReadPreference readPref,
                             final DBDecoder decoder, final DBEncoder encoder) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    @Deprecated
    public void createIndex(final DBObject keys, final DBObject options, final DBEncoder encoder) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public Cursor aggregate(final List<DBObject> pipeline, final AggregationOptions options, final ReadPreference readPreference) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public List<Cursor> parallelScan(final ParallelScanOptions options) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    BulkWriteResult executeBulkWriteOperation(final boolean ordered, final List<WriteRequest> requests, final WriteConcern writeConcern,
                                              final DBEncoder encoder) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public DBCursor find(final DBObject ref, final DBObject keys) {
        return stubDBCursor; 
    }

    public void setDBCursor(final StubDBCursor stubDBCursor) {
        this.stubDBCursor = stubDBCursor;
    }

}
