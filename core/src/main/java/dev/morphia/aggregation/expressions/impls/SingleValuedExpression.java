package dev.morphia.aggregation.expressions.impls;

import com.mongodb.lang.Nullable;
import dev.morphia.MorphiaDatastore;
import dev.morphia.annotations.internal.MorphiaInternal;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

/**
 * @morphia.internal
 * @since 2.3
 */
@MorphiaInternal
public interface SingleValuedExpression {

    @Nullable
    default Expression value() {
        throw new UnsupportedOperationException();
    }

    default void encode(MorphiaDatastore datastore, BsonWriter writer, EncoderContext encoderContext) {
        throw new UnsupportedOperationException();
    }


}
