package dev.morphia.mapping.codec.filters;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.JsonSchemaFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.value;

public class JsonSchemaFilterCodec extends BaseFilterCodec<JsonSchemaFilter> {
    public JsonSchemaFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, JsonSchemaFilter filter, EncoderContext encoderContext) {
        value(datastore.getCodecRegistry(), writer, "$jsonSchema", filter.schema(), encoderContext);
    }

    @Override
    public Class<JsonSchemaFilter> getEncoderClass() {
        return JsonSchemaFilter.class;
    }
}
