package dev.morphia.mapping.experimental;

import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.PropertyCodec;
import dev.morphia.mapping.codec.pojo.PropertyHandler;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.pojo.TypeData;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.sofia.Sofia;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static dev.morphia.mapping.codec.references.ReferenceCodec.processId;

/**
 * Defines a codec for MorphiaReference values
 */
@SuppressWarnings("unchecked")
public class MorphiaReferenceCodec extends PropertyCodec<MorphiaReference> implements PropertyHandler {

    private final Mapper mapper;
    private final BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();

    /**
     * Creates a codec
     *
     * @param datastore     the datastore
     * @param propertyModel the reference property model
     */
    public MorphiaReferenceCodec(Datastore datastore, PropertyModel propertyModel) {
        super(datastore, propertyModel);
        mapper = datastore.getMapper();
    }

    @Override
    public MorphiaReference decode(BsonReader reader, DecoderContext decoderContext) {
        Mapper mapper = getDatastore().getMapper();
        Object value = mapper.getCodecRegistry()
                             .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                             .decode(reader, decoderContext);
        value = processId(value, mapper, decoderContext);
        TypeData typeData = (TypeData) getTypeData().getTypeParameters().get(0);
        if (Set.class.isAssignableFrom(typeData.getType())) {
            return new SetReference<>(getDatastore(), getEntityModelForField(), (List) value);
        } else if (Collection.class.isAssignableFrom(typeData.getType())) {
            return new ListReference<>(getDatastore(), getEntityModelForField(), (List) value);
        } else if (Map.class.isAssignableFrom(typeData.getType())) {
            return new MapReference<>(getDatastore(), (Map) value, getEntityModelForField());
        } else {
            return new SingleReference<>(getDatastore(), getEntityModelForField(), value);
        }
    }

    @Override
    public Object encode(Object value) {
        MorphiaReference<Object> wrap;
        if (value instanceof MorphiaReference) {
            wrap = (MorphiaReference<Object>) value;
        } else {
            wrap = MorphiaReference.wrap(value);
        }
        DocumentWriter writer = new DocumentWriter();
        document(writer, () -> {
            writer.writeName("ref");
            encode(writer, wrap, EncoderContext.builder().build());
        });
        return writer.getDocument().get("ref");
    }

    @Override
    public void encode(BsonWriter writer, MorphiaReference value, EncoderContext encoderContext) {
        Object ids = value.getId(mapper, getDatastore(), getEntityModelForField());
        if (ids == null
            || (ids instanceof Collection && ((Collection<?>) ids).isEmpty())
            || (ids instanceof Map && ((Map<?, ?>) ids).isEmpty())) {
            throw new ReferenceException(Sofia.noIdForReference());
        }

        Codec codec = mapper.getCodecRegistry().get(ids.getClass());
        codec.encode(writer, ids, encoderContext);
    }

    @Override
    public Class getEncoderClass() {
        return MorphiaReference.class;
    }
}
