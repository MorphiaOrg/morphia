package dev.morphia.mapping.codec;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.updates.BaseOperatorCodec;
import dev.morphia.mapping.codec.updates.PullOperatorCodec;
import dev.morphia.mapping.codec.updates.PushOperatorCodec;
import dev.morphia.query.filters.Filter;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class MorphiaUpdateOperatorCodecProvider implements CodecProvider {
    protected final MorphiaDatastore datastore;
    private final Map<Class<?>, BaseOperatorCodec<?>> codecs = new HashMap<>();

    public MorphiaUpdateOperatorCodecProvider(MorphiaDatastore datastore) {
        this.datastore = datastore;
        addCodec(new PullOperatorCodec(datastore));
        addCodec(new PushOperatorCodec(datastore));
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        Codec<T> codec = (Codec<T>) codecs.get(clazz);

        if (codec == null && Filter.class.isAssignableFrom(clazz)) {
            throw new UnsupportedOperationException(clazz.getName() + " needs a codec");
        }
        return codec;
    }

    private void addCodec(BaseOperatorCodec<?> codec) {
        codecs.put(codec.getEncoderClass(), codec);
    }

}
