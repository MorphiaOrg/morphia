package dev.morphia.mapping.codec.updates;

import dev.morphia.MorphiaDatastore;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.updates.PullOperator;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.CodecHelper.document;
import static dev.morphia.mapping.codec.CodecHelper.namedValue;
import static dev.morphia.mapping.codec.CodecHelper.unnamedValue;

public class PullOperatorCodec extends BaseOperatorCodec<PullOperator> {
    public PullOperatorCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void encode(BsonWriter writer, PullOperator operator, EncoderContext encoderContext) {
        document(writer, () -> {
            document(writer, operator.operator(), () -> {
                Object value = operator.value();
                if (value instanceof Filter[] filters) {
                    if (filters.length == 1 && filters[0].getField().equals("")) {
                        namedValue(writer, datastore, operator.field(), filters[0], encoderContext);
                    } else {
                        document(writer, operator.field(), () -> {
                            for (Filter filter : filters) {
                                unnamedValue(writer, datastore, filter, encoderContext);
                            }
                        });
                    }
                } else {
                    namedValue(writer, datastore, operator.field(), value, encoderContext);
                }
            });
        });

    }

    @Override
    public Class<PullOperator> getEncoderClass() {
        return PullOperator.class;
    }
}
