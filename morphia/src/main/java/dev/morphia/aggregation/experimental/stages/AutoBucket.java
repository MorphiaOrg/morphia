package dev.morphia.aggregation.experimental.stages;

import com.mongodb.client.model.BucketGranularity;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.expressions.Fields;

public class AutoBucket extends Stage {
    private Expression groupBy;
    private Integer buckets;
    private Fields<AutoBucket> output;
    private BucketGranularity granularity;

    protected AutoBucket() {
        super("$bucketAuto");
    }

    public static AutoBucket of() {
        return new AutoBucket();
    }

    public AutoBucket buckets(final Integer buckets) {
        this.buckets = buckets;
        return this;
    }

    public Integer getBuckets() {
        return buckets;
    }

    public BucketGranularity getGranularity() {
        return granularity;
    }

    public Expression getGroupBy() {
        return groupBy;
    }

    public Fields<AutoBucket> getOutput() {
        return output;
    }

    public AutoBucket granularity(final BucketGranularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public AutoBucket groupBy(final Expression groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public AutoBucket outputField(final String name, final Expression value) {
        if (output == null) {
            output = Expression.fields(this);
        }
        return output.add(name, value);
    }
}
