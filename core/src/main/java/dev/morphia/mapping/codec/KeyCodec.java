package dev.morphia.mapping.codec;

import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Iterator;

import static dev.morphia.aggregation.codecs.ExpressionHelper.document;

/**
 * Defines the codec for Key types
 */
@SuppressWarnings("unchecked")
@Deprecated(since = "2.0", forRemoval = true)
public class KeyCodec implements Codec<Key> {

    private final Datastore datastore;

    KeyCodec(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Key decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();

        final String ref = reader.readString("$ref");
        reader.readName();
        final BsonReaderMark mark = reader.getMark();
        Object idValue = null;
        EntityModel model = null;
        final Iterator<EntityModel> iterator = datastore.getMapper().getClassesMappedToCollection(ref).iterator();
        while (idValue == null && iterator.hasNext()) {
            model = iterator.next();
            try {
                final PropertyModel idField = model.getIdProperty();
                if (idField != null) {
                    final Class<?> idType = idField.getType();
                    idValue = datastore.getCodecRegistry().get(idType).decode(reader, decoderContext);
                }
            } catch (Exception e) {
                mark.reset();
            }
        }

        if (idValue == null) {
            throw new MappingException("Could not map the Key to a type.");
        }
        reader.readEndDocument();
        return new Key<>(model.getType(), ref, idValue);
    }

    @Override
    public Class<Key> getEncoderClass() {
        return Key.class;
    }

    @Override
    public void encode(BsonWriter writer, Key value, EncoderContext encoderContext) {
        document(writer, () -> {
            String collection = value.getCollection();
            if (collection == null) {
                collection = datastore.getMapper().getEntityModel(value.getType()).getCollectionName();
            }
            writer.writeString("$ref", collection);
            writer.writeName("$id");
            Codec codec = datastore.getCodecRegistry().get(value.getId().getClass());
            codec.encode(writer, value.getId(), encoderContext);
        });
    }
}
