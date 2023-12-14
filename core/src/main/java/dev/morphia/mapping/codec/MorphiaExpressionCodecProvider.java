package dev.morphia.mapping.codec;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.mapping.codec.expressions.AccumulatorCodec;
import dev.morphia.mapping.codec.expressions.AccumulatorExpressionCodec;
import dev.morphia.mapping.codec.expressions.ArrayFilterExpressionCodec;
import dev.morphia.mapping.codec.expressions.ArrayIndexExpressionCodec;
import dev.morphia.mapping.codec.expressions.ArrayLiteralCodec;
import dev.morphia.mapping.codec.expressions.BaseExpressionCodec;
import dev.morphia.mapping.codec.expressions.CalculusExpressionCodec;
import dev.morphia.mapping.codec.expressions.ConvertExpressionCodec;
import dev.morphia.mapping.codec.expressions.CountExpressionCodec;
import dev.morphia.mapping.codec.expressions.DateDeltaExpressionCodec;
import dev.morphia.mapping.codec.expressions.DateDiffExpressionCodec;
import dev.morphia.mapping.codec.expressions.DateExpressionCodec;
import dev.morphia.mapping.codec.expressions.DateFromPartsCodec;
import dev.morphia.mapping.codec.expressions.DateFromStringCodec;
import dev.morphia.mapping.codec.expressions.DateToPartsCodec;
import dev.morphia.mapping.codec.expressions.DateToStringCodec;
import dev.morphia.mapping.codec.expressions.DateTruncExpressionCodec;
import dev.morphia.mapping.codec.expressions.DenseRankExpressionCodec;
import dev.morphia.mapping.codec.expressions.DocumentExpressionCodec;
import dev.morphia.mapping.codec.expressions.DocumentNumberExpressionCodec;
import dev.morphia.mapping.codec.expressions.EndResultsExpressionCodec;
import dev.morphia.mapping.codec.expressions.ExpMovingAvgCodec;
import dev.morphia.mapping.codec.expressions.ExpressionCodec;
import dev.morphia.mapping.codec.expressions.ExpressionListCodec;
import dev.morphia.mapping.codec.expressions.FilterExpressionCodec;
import dev.morphia.mapping.codec.expressions.FunctionExpressionCodec;
import dev.morphia.mapping.codec.expressions.GetFieldExpressionCodec;
import dev.morphia.mapping.codec.expressions.IfNullCodec;
import dev.morphia.mapping.codec.expressions.IndexExpressionCodec;
import dev.morphia.mapping.codec.expressions.IsoDatesCodec;
import dev.morphia.mapping.codec.expressions.LetExpressionCodec;
import dev.morphia.mapping.codec.expressions.LiteralExpressionCodec;
import dev.morphia.mapping.codec.expressions.LogicalExpressionCodec;
import dev.morphia.mapping.codec.expressions.MapExpressionCodec;
import dev.morphia.mapping.codec.expressions.MathExpressionCodec;
import dev.morphia.mapping.codec.expressions.MedianExpressionCodec;
import dev.morphia.mapping.codec.expressions.MergeObjectsCodec;
import dev.morphia.mapping.codec.expressions.MetaExpressionCodec;
import dev.morphia.mapping.codec.expressions.NRankedResultsExpressionCodec;
import dev.morphia.mapping.codec.expressions.PercentileExpressionCodec;
import dev.morphia.mapping.codec.expressions.PushCodec;
import dev.morphia.mapping.codec.expressions.RangeExpressionCodec;
import dev.morphia.mapping.codec.expressions.RankExpressionCodec;
import dev.morphia.mapping.codec.expressions.RankedResultsExpressionCodec;
import dev.morphia.mapping.codec.expressions.ReduceExpressionCodec;
import dev.morphia.mapping.codec.expressions.RegexExpressionCodec;
import dev.morphia.mapping.codec.expressions.ReplaceExpressionCodec;
import dev.morphia.mapping.codec.expressions.SetFieldExpressionCodec;
import dev.morphia.mapping.codec.expressions.ShiftExpressionCodec;
import dev.morphia.mapping.codec.expressions.SliceExpressionCodec;
import dev.morphia.mapping.codec.expressions.SortArrayExpressionCodec;
import dev.morphia.mapping.codec.expressions.SwitchExpressionCodec;
import dev.morphia.mapping.codec.expressions.TrimExpressionCodec;
import dev.morphia.mapping.codec.expressions.UnsetFieldExpressionCodec;
import dev.morphia.mapping.codec.expressions.ValueExpressionCodec;
import dev.morphia.mapping.codec.expressions.ZipExpressionCodec;

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
        addCodec(new CountExpressionCodec(datastore));
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
        addCodec(new FilterExpressionCodec(datastore));
        addCodec(new GetFieldExpressionCodec(datastore));
        addCodec(new FunctionExpressionCodec(datastore));
        addCodec(new IfNullCodec(datastore));
        addCodec(new IndexExpressionCodec(datastore));
        addCodec(new IsoDatesCodec(datastore));
        addCodec(new LetExpressionCodec(datastore));
        addCodec(new LiteralExpressionCodec(datastore));
        addCodec(new LogicalExpressionCodec(datastore));
        addCodec(new MapExpressionCodec(datastore));
        addCodec(new MathExpressionCodec(datastore));
        addCodec(new MedianExpressionCodec(datastore));
        addCodec(new MergeObjectsCodec(datastore));
        addCodec(new MetaExpressionCodec(datastore));
        addCodec(new NRankedResultsExpressionCodec(datastore));
        addCodec(new PercentileExpressionCodec(datastore));
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

    private void addCodec(BaseExpressionCodec<?> codec) {
        codecs.put(codec.getEncoderClass(), codec);
    }

}
