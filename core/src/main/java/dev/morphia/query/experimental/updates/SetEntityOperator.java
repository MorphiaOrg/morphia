package dev.morphia.query.experimental.updates;

import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.DocumentWriter;
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
            public Object encode(Mapper mapper) {
                MappedClass mappedClass = mapper.getMappedClass(getValue().getClass());
                if (mappedClass.getVersionField() == null) {
                    return super.encode(mapper);
                }

                Codec codec = mapper.getCodecRegistry().get(getValue().getClass());
                DocumentWriter writer = new DocumentWriter();

                codec.encode(writer, getValue(), EncoderContext.builder().build());

                Document document = writer.getDocument();
                document.remove(mappedClass.getVersionField().getMappedName());
                return document;
            }
        };
    }
}
