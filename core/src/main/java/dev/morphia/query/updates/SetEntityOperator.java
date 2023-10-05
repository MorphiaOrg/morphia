package dev.morphia.query.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.OperationTarget;

import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @since 2.0
 */
@MorphiaInternal
class SetEntityOperator extends UpdateOperator {
    /**
     * @param value the value
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public SetEntityOperator(Object value) {
        super("$set", "", value);
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public OperationTarget toOperationTarget(MorphiaDatastore datastore, EntityModel model, boolean validate) {
        var pathTarget = new PathTarget(datastore.getMapper(), model, field(), validate);

        return new OperationTarget(null, value()) {
            @Override
            @SuppressWarnings("unchecked")
            public Object encode(MorphiaDatastore datastore) {
                Object value = value();
                EntityModel entityModel = datastore.getMapper().getEntityModel(value.getClass());
                PropertyModel versionProperty = entityModel.getVersionProperty();
                if (versionProperty == null) {
                    return super.encode(datastore);
                }

                Codec<Object> codec = datastore.getCodecRegistry().get((Class<Object>) value.getClass());
                DocumentWriter writer = new DocumentWriter(datastore.getMapper().getConfig());

                codec.encode(writer, value, EncoderContext.builder().build());

                Document document = writer.getDocument();
                document.remove(versionProperty.getMappedName());
                return document;
            }
        };
    }
}
