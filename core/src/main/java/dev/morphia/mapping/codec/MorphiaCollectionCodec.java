package dev.morphia.mapping.codec;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;

import java.util.Collection;

class MorphiaCollectionCodec<T> extends CollectionCodec<T> {
    MorphiaCollectionCodec(TypeWithTypeParameters<T> type,
                           PropertyCodecRegistry registry,
                           TypeWithTypeParameters<T> valueType) {

        super((Class<Collection<T>>) type.getType(), registry.get(valueType));
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
