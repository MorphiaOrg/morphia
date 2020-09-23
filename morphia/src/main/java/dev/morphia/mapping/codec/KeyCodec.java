package dev.morphia.mapping.codec;

import dev.morphia.Key;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Iterator;
import java.util.List;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * Defines the codec for Key types
 */
@SuppressWarnings("unchecked")
@Deprecated(since = "2.0", forRemoval = true)
public class KeyCodec implements Codec<Key> {

    private final Mapper mapper;

    KeyCodec(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void encode(BsonWriter writer, Key value, EncoderContext encoderContext) {
        document(writer, () -> {
            String collection = value.getCollection();
            if (collection == null) {
                collection = mapper.getMappedClass(value.getType()).getCollectionName();
            }
            writer.writeString("$ref", collection);
            writer.writeName("$id");
            Codec codec = mapper.getCodecRegistry().get(value.getId().getClass());
            codec.encode(writer, value.getId(), encoderContext);
        });
    }

    @Override
    public Class<Key> getEncoderClass() {
        return Key.class;
    }

    @Override
    public Key decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();

        final String ref = reader.readString("$ref");
        final List<MappedClass> classes = mapper.getClassesMappedToCollection(ref);
        reader.readName();
        final BsonReaderMark mark = reader.getMark();
        final Iterator<MappedClass> iterator = classes.iterator();
        Object idValue = null;
        MappedClass mappedClass = null;
        while (idValue == null && iterator.hasNext()) {
            mappedClass = iterator.next();
            try {
                final MappedField idField = mappedClass.getIdField();
                if (idField != null) {
                    final Class<?> idType = idField.getTypeData().getType();
                    idValue = mapper.getCodecRegistry().get(idType).decode(reader, decoderContext);
                }
            } catch (Exception e) {
                mark.reset();
            }
        }

        if (idValue == null) {
            throw new MappingException("Could not map the Key to a type.");
        }
        reader.readEndDocument();
        return new Key<>(mappedClass.getType(), ref, idValue);
    }
}
