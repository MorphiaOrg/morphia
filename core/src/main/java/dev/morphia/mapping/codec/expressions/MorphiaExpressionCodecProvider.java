package dev.morphia.mapping.codec.expressions;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;

import dev.morphia.aggregation.expressions.impls.MathExpression;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * @hidden
 * @morphia.internal
 * @since 3.0
 */
public class MorphiaExpressionCodecProvider implements CodecProvider {
    private final Map<Class<?>, BaseExpressionCodec<?>> codecs = new HashMap<>();

    public MorphiaExpressionCodecProvider(MorphiaDatastore datastore) {
        addCodec(new AccumulatorCodec(datastore));
        addCodec(new ArrayFilterExpressionCodec(datastore));
        addCodec(new ArrayIndexExpressionCodec(datastore));
        addCodec(new ArrayLiteralCodec(datastore));
        addCodec(new ConvertExpressionCodec(datastore));
        addCodec(new DateExpressionCodec(datastore));
        addCodec(new DocumentExpressionCodec(datastore));
        addCodec(new EndResultsExpressionCodec(datastore));
        addCodec(new ExpressionCodec(datastore));
        addCodec(new ExpressionListCodec(datastore));
        addCodec(new FunctionExpressionCodec(datastore));
        addCodec(new IfNullCodec(datastore));
        addCodec(new IndexExpressionCodec(datastore));
        addCodec(new LetExpressionCodec(datastore));
        addCodec(new LiteralExpressionCodec(datastore));
        addCodec(new LogicalExpressionCodec(datastore));
        addCodec(new MapExpressionCodec(datastore));
        addCodec(new MathExpressionCodec(datastore));
        addCodec(new MergeObjectsCodec(datastore));
        addCodec(new MetaExpressionCodec(datastore));
        addCodec(new NRankedResultsExpressionCodec(datastore));
        addCodec(new PushCodec(datastore));
        addCodec(new RankedResultsExpressionCodec(datastore));
        addCodec(new ReduceExpressionCodec(datastore));
        addCodec(new RegexExpressionCodec(datastore));
        addCodec(new ReplaceExpressionCodec(datastore));
        addCodec(new SliceExpressionCodec(datastore));
        addCodec(new SortArrayExpressionCodec(datastore));
        addCodec(new SwitchExpressionCodec(datastore));
        addCodec(new TrimExpressionCodec(datastore));
        addCodec(new ValueExpressionCodec(datastore));
        addCodec(new ZipExpressionCodec(datastore));
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        Codec<T> codec = (Codec<T>) codecs.get(clazz);

        if (codec == null && Expression.class.isAssignableFrom(clazz)) {
            throw new UnsupportedOperationException(clazz.getName() + " needs a codec");
        }
        return codec;
    }

    protected void addCodec(BaseExpressionCodec<?> codec) {
        codecs.put(codec.getEncoderClass(), codec);
    }

}
