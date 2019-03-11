package dev.morphia.query;


import com.mongodb.WriteResult;

import static java.lang.String.format;

/**
 * This class holds various metrics about the results of an update operation.
 */
public class  UpdateResults {
    private final WriteResult wr;

    /**
     * Creates an UpdateResults
     *
     * @param wr the WriteResult from the driver.
     */
    public UpdateResults(final WriteResult wr) {
        this.wr = wr;
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
     * @return number updated
     */
    public int getUpdatedCount() {
        return getUpdatedExisting() ? getN() : 0;
    }

    /**
     * @return true if updated, false if inserted or none effected
     */
    public boolean getUpdatedExisting() {
        return wr.isUpdateOfExisting();
    }

    /**
     * @return the underlying data
     */
    public WriteResult getWriteResult() {
        return wr;
    }

    /**
     * @return number of affected documents
     */
    protected int getN() {
        return wr.getN();
    }

    @Override
    public String toString() {
        return format("UpdateResults{wr=%s}", wr);
    }
}
