package dev.morphia.mapping.codec.expressions;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;

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
        addCodec(new AccumulatorExpressionCodec(datastore));
        addCodec(new ArrayFilterExpressionCodec(datastore));
        addCodec(new ArrayIndexExpressionCodec(datastore));
        addCodec(new ArrayLiteralCodec(datastore));
        addCodec(new CalculusExpressionCodec(datastore));
        addCodec(new ConvertExpressionCodec(datastore));
        addCodec(new DateDeltaExpressionCodec(datastore));
        addCodec(new DateDiffExpressionCodec(datastore));
        addCodec(new DateExpressionCodec(datastore));
        addCodec(new DateFromPartsCodec(datastore));
        addCodec(new DateFromStringCodec(datastore));
        addCodec(new DateToPartsCodec(datastore));
        addCodec(new DateToStringCodec(datastore));
        addCodec(new DateTruncExpressionCodec(datastore));
        addCodec(new DenseRankExpressionCodec(datastore));
        addCodec(new DocumentExpressionCodec(datastore));
        addCodec(new DocumentNumberExpressionCodec(datastore));
        addCodec(new EndResultsExpressionCodec(datastore));
        addCodec(new ExpMovingAvgCodec(datastore));
        addCodec(new ExpressionCodec(datastore));
        addCodec(new ExpressionListCodec(datastore));
        addCodec(new FunctionExpressionCodec(datastore));
        addCodec(new IfNullCodec(datastore));
        addCodec(new IndexExpressionCodec(datastore));
        addCodec(new IsoDatesCodec(datastore));
        addCodec(new LetExpressionCodec(datastore));
        addCodec(new LiteralExpressionCodec(datastore));
        addCodec(new LogicalExpressionCodec(datastore));
        addCodec(new MapExpressionCodec(datastore));
        addCodec(new MathExpressionCodec(datastore));
        addCodec(new MergeObjectsCodec(datastore));
        addCodec(new MetaExpressionCodec(datastore));
        addCodec(new NRankedResultsExpressionCodec(datastore));
        addCodec(new PushCodec(datastore));
        addCodec(new RangeExpressionCodec(datastore));
        addCodec(new RankExpressionCodec(datastore));
        addCodec(new RankedResultsExpressionCodec(datastore));
        addCodec(new ReduceExpressionCodec(datastore));
        addCodec(new RegexExpressionCodec(datastore));
        addCodec(new ReplaceExpressionCodec(datastore));
        addCodec(new SetFieldExpressionCodec(datastore));
        addCodec(new ShiftExpressionCodec(datastore));
        addCodec(new SliceExpressionCodec(datastore));
        addCodec(new SortArrayExpressionCodec(datastore));
        addCodec(new SwitchExpressionCodec(datastore));
        addCodec(new TrimExpressionCodec(datastore));
        addCodec(new UnsetFieldExpressionCodec(datastore));
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
