package dev.morphia.aggregation.experimental.codecs;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.codecs.stages.AddFieldsCodec;
import dev.morphia.aggregation.experimental.codecs.stages.AutoBucketCodec;
import dev.morphia.aggregation.experimental.codecs.stages.BucketCodec;
import dev.morphia.aggregation.experimental.codecs.stages.CollectionStatsCodec;
import dev.morphia.aggregation.experimental.codecs.stages.CountCodec;
import dev.morphia.aggregation.experimental.codecs.stages.CurrentOpCodec;
import dev.morphia.aggregation.experimental.codecs.stages.FacetCodec;
import dev.morphia.aggregation.experimental.codecs.stages.GeoNearCodec;
import dev.morphia.aggregation.experimental.codecs.stages.GraphLookupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.GroupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.IndexStatsCodec;
import dev.morphia.aggregation.experimental.codecs.stages.LimitCodec;
import dev.morphia.aggregation.experimental.codecs.stages.LookupCodec;
import dev.morphia.aggregation.experimental.codecs.stages.MatchCodec;
import dev.morphia.aggregation.experimental.codecs.stages.MergeCodec;
import dev.morphia.aggregation.experimental.codecs.stages.OutCodec;
import dev.morphia.aggregation.experimental.codecs.stages.PlanCacheStatsCodec;
import dev.morphia.aggregation.experimental.codecs.stages.ProjectionCodec;
import dev.morphia.aggregation.experimental.codecs.stages.RedactCodec;
import dev.morphia.aggregation.experimental.codecs.stages.ReplaceRootCodec;
import dev.morphia.aggregation.experimental.codecs.stages.ReplaceWithCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SampleCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SkipCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SortByCountCodec;
import dev.morphia.aggregation.experimental.codecs.stages.SortCodec;
import dev.morphia.aggregation.experimental.codecs.stages.StageCodec;
import dev.morphia.aggregation.experimental.codecs.stages.UnionWithCodec;
import dev.morphia.aggregation.experimental.codecs.stages.UnsetCodec;
import dev.morphia.aggregation.experimental.codecs.stages.UnwindCodec;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.AddFields;
import dev.morphia.aggregation.experimental.stages.AutoBucket;
import dev.morphia.aggregation.experimental.stages.Bucket;
import dev.morphia.aggregation.experimental.stages.CollectionStats;
import dev.morphia.aggregation.experimental.stages.Count;
import dev.morphia.aggregation.experimental.stages.CurrentOp;
import dev.morphia.aggregation.experimental.stages.Facet;
import dev.morphia.aggregation.experimental.stages.GeoNear;
import dev.morphia.aggregation.experimental.stages.GraphLookup;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.IndexStats;
import dev.morphia.aggregation.experimental.stages.Limit;
import dev.morphia.aggregation.experimental.stages.Lookup;
import dev.morphia.aggregation.experimental.stages.Match;
import dev.morphia.aggregation.experimental.stages.Merge;
import dev.morphia.aggregation.experimental.stages.Out;
import dev.morphia.aggregation.experimental.stages.PlanCacheStats;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Redact;
import dev.morphia.aggregation.experimental.stages.ReplaceRoot;
import dev.morphia.aggregation.experimental.stages.ReplaceWith;
import dev.morphia.aggregation.experimental.stages.Sample;
import dev.morphia.aggregation.experimental.stages.Skip;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.aggregation.experimental.stages.SortByCount;
import dev.morphia.aggregation.experimental.stages.UnionWith;
import dev.morphia.aggregation.experimental.stages.Unset;
import dev.morphia.aggregation.experimental.stages.Unwind;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AggregationCodecProvider implements CodecProvider {

    private final Codec expressionCodec;
    private final Datastore datastore;
    private Map<Class, StageCodec> codecs;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregationCodecProvider(Datastore datastore) {
        this.datastore = datastore;
        expressionCodec = new ExpressionCodec(datastore);
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
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
            codecs.put(AddFields.class, new AddFieldsCodec(datastore));
            codecs.put(AutoBucket.class, new AutoBucketCodec(datastore));
            codecs.put(Bucket.class, new BucketCodec(datastore));
            codecs.put(CollectionStats.class, new CollectionStatsCodec(datastore));
            codecs.put(Count.class, new CountCodec(datastore));
            codecs.put(CurrentOp.class, new CurrentOpCodec(datastore));
            codecs.put(Facet.class, new FacetCodec(datastore));
            codecs.put(GeoNear.class, new GeoNearCodec(datastore));
            codecs.put(GraphLookup.class, new GraphLookupCodec(datastore));
            codecs.put(Group.class, new GroupCodec(datastore));
            codecs.put(IndexStats.class, new IndexStatsCodec(datastore));
            codecs.put(Merge.class, new MergeCodec(datastore));
            codecs.put(PlanCacheStats.class, new PlanCacheStatsCodec(datastore));
            codecs.put(Limit.class, new LimitCodec(datastore));
            codecs.put(Lookup.class, new LookupCodec(datastore));
            codecs.put(Match.class, new MatchCodec(datastore));
            codecs.put(Out.class, new OutCodec(datastore));
            codecs.put(Projection.class, new ProjectionCodec(datastore));
            codecs.put(Redact.class, new RedactCodec(datastore));
            codecs.put(ReplaceRoot.class, new ReplaceRootCodec(datastore));
            codecs.put(ReplaceWith.class, new ReplaceWithCodec(datastore));
            codecs.put(Sample.class, new SampleCodec(datastore));
            codecs.put(Skip.class, new SkipCodec(datastore));
            codecs.put(Sort.class, new SortCodec(datastore));
            codecs.put(SortByCount.class, new SortByCountCodec(datastore));
            codecs.put(UnionWith.class, new UnionWithCodec(datastore));
            codecs.put(Unset.class, new UnsetCodec(datastore));
            codecs.put(Unwind.class, new UnwindCodec(datastore));
        }
        return codecs;
    }
}
