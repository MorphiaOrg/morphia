package org.mongodb.morphia.query;


import com.mongodb.WriteResult;


public class UpdateResults {
    private final WriteResult wr;

    public UpdateResults(final WriteResult wr) {
        this.wr = wr;
    }

    /**
     * @return true if updated, false if inserted or none effected
     */
    public boolean getUpdatedExisting() {
        return wr.isUpdateOfExisting();
    }

    /**
     * @return number updated
     */
    public int getUpdatedCount() {
        return getUpdatedExisting() ? getN() : 0;
    }

    /**
     * @return number of affected documents
     */
    protected int getN() {
        return wr.getN();
    }

    /**
     * @return number inserted; this should be either 0/1.
     */
    public int getInsertedCount() {
        return !getUpdatedExisting() ? getN() : 0;
    }

    /**
     * @return the new _id field if an insert/upsert was performed
     */
    public Object getNewId() {
        return wr.getUpsertedId();
    }

    /**
     * @return the underlying data
     */
    public WriteResult getWriteResult() {
        return wr;
    }
}
