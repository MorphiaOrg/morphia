package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.aggregation.experimental.GraphLookup;
import dev.morphia.aggregation.experimental.Limit;
import dev.morphia.aggregation.experimental.Lookup;
import dev.morphia.aggregation.experimental.codecs.stages.AddFieldsCodec;
import dev.morphia.aggregation.experimental.codecs.stages.AutoBucketCodec;
import dev.morphia.aggregation.experimental.codecs.stages.BucketCodec;
import dev.morphia.aggregation.experimental.codecs.stages.CollectionStatsCodec;
import dev.morphia.aggregation.experimental.codecs.stages.CountCodec;
import dev.morphia.aggregation.experimental.codecs.stages.CurrentOpCodec;
import dev.morphia.aggregation.experimental.codecs.stages.FacetCodec;
import dev.morphia.aggregation.experimental.codecs.stages.GraphLookupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.GroupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.LimitCodec;
import dev.morphia.aggregation.experimental.codecs.stages.LookupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.MatchCodec;
import dev.morphia.aggregation.experimental.codecs.stages.ProjectionCodec;
import dev.morphia.aggregation.experimental.codecs.stages.ReplaceWith;
import dev.morphia.aggregation.experimental.codecs.stages.ReplaceWithCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SampleCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SkipCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SortByCountCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SortCodec;
import dev.morphia.aggregation.experimental.codecs.stages.StageCodec;
import dev.morphia.aggregation.experimental.codecs.stages.UnsetCodec;
import dev.morphia.aggregation.experimental.codecs.stages.UnwindCodec;
import dev.morphia.aggregation.experimental.expressions.Expression;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.Count;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import dev.morphia.mapping.Mapper;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AggregationCodecProvider implements CodecProvider {

    private Map<Class, StageCodec> codecs;
    private Codec expressionCodec;
    private Mapper mapper;

    public AggregationCodecProvider(final Mapper mapper) {
        this.mapper = mapper;
        expressionCodec = new ExpressionCodec(this.mapper);
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        Codec<T> codec = getCodecs().get(clazz);
        if (codec == null) {
            if (Expression.class.isAssignableFrom(clazz)) {
                codec = expressionCodec;
            }
        }
        return codec;
    }

    private Map<Class, StageCodec> getCodecs() {
        if (codecs == null) {
            codecs = new HashMap<>();

            // Stages
            codecs.put(AddFields.class, new AddFieldsCodec(mapper));
            codecs.put(AutoBucket.class, new AutoBucketCodec(mapper));
            codecs.put(Bucket.class, new BucketCodec(mapper));
            codecs.put(CollectionStats.class, new CollectionStatsCodec(mapper));
            codecs.put(Count.class, new CountCodec(mapper));
            codecs.put(CurrentOp.class, new CurrentOpCodec(mapper));
            codecs.put(Facet.class, new FacetCodec(mapper));
            codecs.put(GraphLookup.class, new GraphLookupCodec(mapper));
            codecs.put(Group.class, new GroupCodec(mapper));
            codecs.put(Limit.class, new LimitCodec(mapper));
            codecs.put(Lookup.class, new LookupCodec(mapper));
            codecs.put(Match.class, new MatchCodec(mapper));
            codecs.put(Projection.class, new ProjectionCodec(mapper));
            codecs.put(ReplaceWith.class, new ReplaceWithCodec(mapper));
            codecs.put(Sample.class, new SampleCodec(mapper));
            codecs.put(Skip.class, new SkipCodec(mapper));
            codecs.put(Sort.class, new SortCodec(mapper));
            codecs.put(SortByCount.class, new SortByCountCodec(mapper));
            codecs.put(Unset.class, new UnsetCodec(mapper));
            codecs.put(Unwind.class, new UnwindCodec(mapper));
        }
        return codecs;
    }
}
