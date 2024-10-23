package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.query.updates.SetEntityOperator;

import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SetEntityOperatorCodec extends BaseOperatorCodec<SetEntityOperator> {
    public SetEntityOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public void encode(BsonWriter writer, SetEntityOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            DocumentWriter documentWriter = new DocumentWriter(datastore.getMapper().getConfig());
            Codec codec = datastore.getCodecRegistry().get(operator.value().getClass());
            encoderContext.encodeWithChildContext(codec, documentWriter, operator.value());
            Document document = documentWriter.getDocument();
            EntityModel model = operator.model();
            if (model != null && model.getVersionProperty() != null) {
                document.remove(model.getVersionProperty().getMappedName());
            }

            namedValue(writer, datastore, "$set", document, encoderContext);
        });
    }

    @Override
    public Class<SetEntityOperator> getEncoderClass() {
        return SetEntityOperator.class;
    }
}
