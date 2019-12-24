package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.Limit;
import dev.morphia.aggregation.experimental.stages.Accumulator;
import dev.morphia.aggregation.experimental.stages.DateExpression.DateToStringExpression;
import dev.morphia.aggregation.experimental.stages.Expression;
import dev.morphia.aggregation.experimental.stages.Expression.Literal;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.mapping.Mapper;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AggregationCodecProvider implements CodecProvider {

    private Map<Class, Codec> codecs;
    private Mapper mapper;

    public AggregationCodecProvider(final Mapper mapper) {
        this.mapper = mapper;
    }

    private Map<Class, Codec> getCodecs() {
        if (codecs == null) {
            CodecRegistry codecRegistry = mapper.getCodecRegistry();
            codecs = new HashMap<>();
            codecs.put(Group.class, new GroupCodec(codecRegistry));
            codecs.put(Accumulator.class, new ExpressionCodec(codecRegistry));
            codecs.put(Expression.class, new ExpressionCodec(codecRegistry));
            codecs.put(Literal.class, new ExpressionLiteralCodec(codecRegistry));
            codecs.put(Sample.class, new SampleCodec());
            codecs.put(DateToStringExpression.class, new DateToStringExpressionCodec(codecRegistry));
            codecs.put(Projection.class, new ProjectionCodec(codecRegistry));
            codecs.put(Sort.class, new SortCodec());
            codecs.put(Limit.class, new LimitCodec());
        }
        return codecs;
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        return (Codec<T>) getCodecs().get(clazz);
    }
}
