package dev.morphia.aggregation.experimental.codecs.stages;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.experimental.stages.Bucket;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.expression;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.value;

public class BucketCodec extends StageCodec<Bucket> {
    public BucketCodec(Datastore datastore) {
        super(datastore);
    }

    @Override
    public Class getEncoderClass() {
        return Bucket.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, Bucket value, EncoderContext encoderContext) {
        document(writer, () -> {
            expression(getDatastore(), writer, "groupBy", value.getGroupBy(), encoderContext);
            value(getDatastore(), writer, "boundaries", value.getBoundaries(), encoderContext);
            value(getDatastore(), writer, "default", value.getDefaultValue(), encoderContext);
            DocumentExpression output = value.getOutput();
            if (output != null) {
                output.encode("output", getDatastore(), writer, encoderContext);
            }
        });
    }
}
