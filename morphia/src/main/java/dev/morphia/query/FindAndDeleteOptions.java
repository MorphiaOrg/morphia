package dev.morphia.query;

import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

public class FindAndDeleteOptions extends FindOneAndDeleteOptions {
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;

    public WriteConcern writeConcern() {
        return writeConcern;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public FindAndDeleteOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * @inheritDoc
     */
    @Override
    public FindAndDeleteOptions projection(final Bson projection) {
        super.projection(projection);
        return this;
    }

    /**
     * @inheritDoc
     */
    @Override
    public FindAndDeleteOptions sort(final Bson sort) {
        super.sort(sort);
        return this;
    }

    /**
     * @inheritDoc
     */
    public FindAndDeleteOptions sort(final Document sort) {
        super.sort(sort);
        return this;
    }

    /**
     * @inheritDoc
     */
    @Override
    public FindAndDeleteOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    /**
     * @inheritDoc
     */
    @Override
    public FindAndDeleteOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }
}
