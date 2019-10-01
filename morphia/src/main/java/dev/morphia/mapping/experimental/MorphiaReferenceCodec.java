package dev.morphia.mapping.experimental;

import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.PropertyCodec;
import dev.morphia.mapping.codec.references.ReferenceCodec;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.TypeData;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.mapping.codec.references.ReferenceCodec.processId;

@SuppressWarnings("unchecked")
public class MorphiaReferenceCodec extends PropertyCodec<MorphiaReference> {

    private final Mapper mapper;
    private BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();

    public MorphiaReferenceCodec(final Datastore datastore, final Field field, final String name,
                                 final TypeData typeData) {
        super(datastore, field, name, (TypeData) typeData.getTypeParameters().get(0));
        mapper = datastore.getMapper();
    }

    @Override
    public Class<MorphiaReference> getEncoderClass() {
        return MorphiaReference.class;
    }

    @Override
    public MorphiaReference decode(final BsonReader reader, final DecoderContext decoderContext) {
        Mapper mapper = getDatastore().getMapper();
        Object value = mapper.getCodecRegistry()
                             .get(bsonTypeClassMap.get(reader.getCurrentBsonType()))
                             .decode(reader, decoderContext);
        value = processId(value, mapper, decoderContext);
        if (Set.class.isAssignableFrom(getTypeData().getType())) {
            return new SetReference<>(getDatastore(), getFieldMappedClass(), (List) value);
        } else if (Collection.class.isAssignableFrom(getTypeData().getType())) {
            return new ListReference<>(getDatastore(), getFieldMappedClass(), (List) value);
        } else if (Map.class.isAssignableFrom(getTypeData().getType())) {
            return new MapReference<>(getDatastore(), getFieldMappedClass(), (Map) value);
        } else {
            return new SingleReference<>(mapper.getDatastore(), getFieldMappedClass(), value);
        }
    }

    @Override
    public void encode(final BsonWriter writer, final MorphiaReference value, final EncoderContext encoderContext) {
        Object ids = value.getId(mapper, getFieldMappedClass());
        Codec codec = mapper.getCodecRegistry().get(ids.getClass());
        codec.encode(writer, ids, encoderContext);
    }
}
