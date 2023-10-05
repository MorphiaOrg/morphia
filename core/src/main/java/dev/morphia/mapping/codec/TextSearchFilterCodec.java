package dev.morphia.mapping.codec;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.query.filters.TextSearchFilter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.value;

public class TextSearchFilterCodec extends BaseFilterCodec<TextSearchFilter> {
    public TextSearchFilterCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, TextSearchFilter filter, EncoderContext context) {
        document(writer, filter.getName(), () -> {
            value(writer, "$search", filter.searchText());
            value(writer, "$language", filter.language());
            if (Boolean.TRUE.equals(filter.caseSensitive())) {
                value(writer, "$caseSensitive", filter.caseSensitive());
            }
            if (Boolean.TRUE.equals(filter.diacriticSensitive())) {
                value(writer, "$diacriticSensitive", filter.diacriticSensitive());
            }
        });
    }

    @Override
    public Class<TextSearchFilter> getEncoderClass() {
        return TextSearchFilter.class;
    }
}
