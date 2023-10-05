package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.expressions.impls.Fields;
import dev.morphia.aggregation.expressions.impls.PipelineField;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static dev.morphia.mapping.codec.CodecHelper.encodeIfNotNull;

public class FieldsCodec implements Codec<Fields> {
    private final MorphiaDatastore datastore;

    public FieldsCodec(MorphiaDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Fields decode(BsonReader reader, DecoderContext decoderContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encode(BsonWriter writer, Fields fields, EncoderContext encoderContext) {
        CodecRegistry registry = datastore.getCodecRegistry();
        for (PipelineField field : fields.getFields()) {
            String name = field.getName();
            Expression value = field.getValue();
            encodeIfNotNull(registry, writer, name, value, encoderContext);
        }

    }

    @Override
    public Class<Fields> getEncoderClass() {
        return Fields.class;
    }
}
