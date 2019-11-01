package dev.morphia.query;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.query.CriteriaJoin.AND;

/**
 * Defines the codec for CriteriaContainers
 */
@SuppressWarnings("unchecked")
public class CriteriaContainerCodec implements Codec<CriteriaContainerImpl> {
    private Mapper mapper;

    /**
     * Creates the codec
     *
     * @param mapper the Mapper to use
     */
    public CriteriaContainerCodec(final Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CriteriaContainerImpl decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException(Sofia.persistenceNotIntended());
    }

    @Override
    public void encode(final BsonWriter writer, final CriteriaContainerImpl value, final EncoderContext encoderContext) {
        if (value.getJoinMethod() == AND) {
            and(writer, value, encoderContext);
        } else {
            or(writer, value, encoderContext);
        }

    }

    @Override
    public Class<CriteriaContainerImpl> getEncoderClass() {
        return CriteriaContainerImpl.class;
    }

    private void and(final BsonWriter writer, final CriteriaContainerImpl value, final EncoderContext encoderContext) {
        final List<Document> and = new ArrayList<>();
        Set<String> names = new HashSet<>();
        boolean duplicates = false;

        for (final Criteria child : value.getChildren()) {
            final Document childObject = getCriteriaDoc(child, encoderContext);
            for (final String s : childObject.keySet()) {
                duplicates |= !names.add(s);
            }
            and.add(childObject);
        }

        Codec<Document> documentCodec = mapper.getCodecRegistry().get(Document.class);

        writer.writeStartDocument();
        if (!duplicates) {
            Document document = new Document();
            for (final Object o : and) {
                document.putAll((Map) o);
            }
            documentCodec.encode(writer, document, encoderContext);
        } else {
            writer.writeStartArray("$and");
            and.forEach(d -> documentCodec.encode(writer, d, encoderContext));
            writer.writeEndArray();
        }
        writer.writeEndDocument();
    }

    private void or(final BsonWriter writer, final CriteriaContainerImpl value, final EncoderContext encoderContext) {

        Codec<Document> documentCodec = mapper.getCodecRegistry().get(Document.class);
        writer.writeStartArray("$or");
        value.getChildren()
             .stream()
             .map(child -> getCriteriaDoc(child, encoderContext))
             .forEach(d -> documentCodec.encode(writer, d, encoderContext));
        writer.writeEndArray();
    }

    private Document getCriteriaDoc(final Criteria child, final EncoderContext encoderContext) {
        return new DocumentWriter()
                   .encode(mapper.getCodecRegistry(), child, encoderContext)
                   .getRoot();
    }
}
