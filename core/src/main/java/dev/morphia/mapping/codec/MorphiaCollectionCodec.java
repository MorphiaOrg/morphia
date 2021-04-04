package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;

import java.util.Collection;

class MorphiaCollectionCodec<T> extends CollectionCodec<T> {
    MorphiaCollectionCodec(Codec<T> codec, Class<T> type) {
        super((Class<Collection<T>>) type, codec);
    }

    @Override
    public Collection<T> decode(BsonReader reader, DecoderContext decoderContext) {
        if (reader.getCurrentBsonType().equals(BsonType.ARRAY)) {
            return super.decode(reader, decoderContext);
        }
        final Collection<T> collection = getInstance();
        T value = getCodec().decode(reader, decoderContext);
        collection.add(value);
        return collection;
    }


}
