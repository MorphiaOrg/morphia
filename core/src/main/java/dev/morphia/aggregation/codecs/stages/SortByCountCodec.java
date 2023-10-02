package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.SortByCount;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.encodeIfNotNull;

public class SortByCountCodec extends StageCodec<SortByCount> {
    public SortByCountCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<SortByCount> getEncoderClass() {
        return SortByCount.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, SortByCount value, EncoderContext encoderContext) {
        encodeIfNotNull(getCodecRegistry(), writer,value.getExpression(), encoderContext);
    }
}
