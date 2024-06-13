package dev.morphia.mapping.codec;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.filters.BaseFilterCodec;
import dev.morphia.mapping.codec.filters.BoxCodec;
import dev.morphia.mapping.codec.filters.CenterFilterCodec;
import dev.morphia.mapping.codec.filters.ElemMatchFilterCodec;
import dev.morphia.mapping.codec.filters.EqFilterCodec;
import dev.morphia.mapping.codec.filters.ExistsFilterCodec;
import dev.morphia.mapping.codec.filters.ExprFilterCodec;
import dev.morphia.mapping.codec.filters.FieldLessFilterCodec;
import dev.morphia.mapping.codec.filters.FilterCodec;
import dev.morphia.mapping.codec.filters.GeoIntersectsFilterCodec;
import dev.morphia.mapping.codec.filters.GeoWithinFilterCodec;
import dev.morphia.mapping.codec.filters.JsonSchemaFilterCodec;
import dev.morphia.mapping.codec.filters.LogicalFilterCodec;
import dev.morphia.mapping.codec.filters.NearFilterCodec;
import dev.morphia.mapping.codec.filters.PolygonFilterCodec;
import dev.morphia.mapping.codec.filters.RegexFilterCodec;
import dev.morphia.mapping.codec.filters.SampleRateFilterCodec;
import dev.morphia.mapping.codec.filters.TextSearchFilterCodec;
import dev.morphia.mapping.codec.filters.WhereFilterCodec;
import dev.morphia.query.filters.Filter;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class MorphiaFilterCodecProvider implements CodecProvider {
    protected final MorphiaDatastore datastore;
    private final Map<Class<?>, BaseFilterCodec<?>> codecs = new HashMap<>();

    public MorphiaFilterCodecProvider(MorphiaDatastore datastore) {
        this.datastore = datastore;
        addCodec(new BoxCodec(datastore));
        addCodec(new CenterFilterCodec(datastore));
        addCodec(new ElemMatchFilterCodec(datastore));
        addCodec(new EqFilterCodec(datastore));
        addCodec(new ExistsFilterCodec(datastore));
        addCodec(new ExprFilterCodec(datastore));
        addCodec(new FilterCodec(datastore));
        addCodec(new FieldLessFilterCodec(datastore));
        addCodec(new GeoIntersectsFilterCodec(datastore));
        addCodec(new GeoWithinFilterCodec(datastore));
        addCodec(new JsonSchemaFilterCodec(datastore));
        addCodec(new LogicalFilterCodec(datastore));
        addCodec(new ModFilterCodec(datastore));
        addCodec(new NearFilterCodec(datastore));
        addCodec(new PolygonFilterCodec(datastore));
        addCodec(new RegexFilterCodec(datastore));
        addCodec(new SampleRateFilterCodec(datastore));
        addCodec(new TextSearchFilterCodec(datastore));
        addCodec(new WhereFilterCodec(datastore));
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        Codec<T> codec = (Codec<T>) codecs.get(clazz);

        if (codec == null && Filter.class.isAssignableFrom(clazz)) {
            throw new UnsupportedOperationException(clazz.getName() + " needs a codec");
        }
        return codec;
    }

    private void addCodec(BaseFilterCodec<?> codec) {
        codecs.put(codec.getEncoderClass(), codec);
    }

}
