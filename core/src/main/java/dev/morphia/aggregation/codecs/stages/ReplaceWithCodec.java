package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.DocumentExpression;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.ReplaceWith;

import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class ReplaceWithCodec extends StageCodec<ReplaceWith> {
    public ReplaceWithCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<ReplaceWith> getEncoderClass() {
        return ReplaceWith.class;
    }

    @Override
    protected void encodeStage(BsonWriter writer, ReplaceWith replace, EncoderContext encoderContext) {
        Expression value = replace.value();
        CodecRegistry codecRegistry = getDatastore().getCodecRegistry();
        if (value != null) {
            encodeIfNotNull(codecRegistry, writer, value, encoderContext);
        } else {
            DocumentExpression document = replace.document();
            Codec codec = codecRegistry.get(document.getClass());
            codec.encode(writer, document, encoderContext);
        }
        // ----
        /*
         * if (value == null) {
         * value = replace.getDocument();
         * }
         * 
         * Codec codec = codecRegistry.get(value.getClass());
         * codec.encode(writer, value, encoderContext);
         */

    }
}
