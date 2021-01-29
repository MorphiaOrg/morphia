package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @since 2.0
 */
public class SetEntityOperator extends UpdateOperator {
    /**
     * @param value the value
     * @morphia.internal
     */
    public SetEntityOperator(Object value) {
        super("$set", "", value);
    }


    @Override
    public OperationTarget toTarget(PathTarget pathTarget) {
        return new OperationTarget(null, value()) {
            @Override
            @SuppressWarnings("unchecked")
            public Object encode(Mapper mapper) {
                EntityModel entityModel = mapper.getEntityModel(getValue().getClass());
                if (entityModel.getVersionProperty() == null) {
                    return super.encode(mapper);
                }

                Codec<Object> codec = mapper.getCodecRegistry().get((Class<Object>) getValue().getClass());
                DocumentWriter writer = new DocumentWriter();

                codec.encode(writer, getValue(), EncoderContext.builder().build());

                Document document = writer.getDocument();
                document.remove(entityModel.getVersionProperty().getMappedName());
                return document;
            }
        };
    }
}
