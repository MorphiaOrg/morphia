package dev.morphia.aggregation.codecs;

import dev.morphia.Datastore;
import dev.morphia.aggregation.codecs.stages.AddFieldsCodec;
import dev.morphia.aggregation.codecs.stages.AutoBucketCodec;
import dev.morphia.aggregation.codecs.stages.BucketCodec;
import dev.morphia.aggregation.codecs.stages.CollectionStatsCodec;
import dev.morphia.aggregation.codecs.stages.CountCodec;
import dev.morphia.aggregation.codecs.stages.CurrentOpCodec;
import dev.morphia.aggregation.codecs.stages.FacetCodec;
import dev.morphia.aggregation.codecs.stages.GeoNearCodec;
import dev.morphia.aggregation.codecs.stages.GraphLookupCodec;
import dev.morphia.aggregation.codecs.stages.GroupCodec;
import dev.morphia.aggregation.codecs.stages.IndexStatsCodec;
import dev.morphia.aggregation.codecs.stages.LimitCodec;
import dev.morphia.aggregation.codecs.stages.LookupCodec;
import dev.morphia.aggregation.codecs.stages.MatchCodec;
import dev.morphia.aggregation.codecs.stages.MergeCodec;
import dev.morphia.aggregation.codecs.stages.OutCodec;
import dev.morphia.aggregation.codecs.stages.PlanCacheStatsCodec;
import dev.morphia.aggregation.codecs.stages.ProjectionCodec;
import dev.morphia.aggregation.codecs.stages.RedactCodec;
import dev.morphia.aggregation.codecs.stages.ReplaceRootCodec;
import dev.morphia.aggregation.codecs.stages.ReplaceWithCodec;
import dev.morphia.aggregation.codecs.stages.SampleCodec;
import dev.morphia.aggregation.codecs.stages.SetStageCodec;
import dev.morphia.aggregation.codecs.stages.SetWindowFieldsCodec;
import dev.morphia.aggregation.codecs.stages.SkipCodec;
import dev.morphia.aggregation.codecs.stages.SortByCountCodec;
import dev.morphia.aggregation.codecs.stages.SortCodec;
import dev.morphia.aggregation.codecs.stages.StageCodec;
import dev.morphia.aggregation.codecs.stages.UnionWithCodec;
import dev.morphia.aggregation.codecs.stages.UnsetCodec;
import dev.morphia.aggregation.codecs.stages.UnwindCodec;
import dev.morphia.aggregation.expressions.impls.Expression;
import dev.morphia.aggregation.stages.AddFields;
import dev.morphia.aggregation.stages.AutoBucket;
import dev.morphia.aggregation.stages.Bucket;
import dev.morphia.aggregation.stages.CollectionStats;
import dev.morphia.aggregation.stages.Count;
import dev.morphia.aggregation.stages.CurrentOp;
import dev.morphia.aggregation.stages.Facet;
import dev.morphia.aggregation.stages.GeoNear;
import dev.morphia.aggregation.stages.GraphLookup;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.aggregation.stages.IndexStats;
import dev.morphia.aggregation.stages.Limit;
import dev.morphia.aggregation.stages.Lookup;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.Merge;
import dev.morphia.aggregation.stages.Out;
import dev.morphia.aggregation.stages.PlanCacheStats;
import dev.morphia.aggregation.stages.Projection;
import dev.morphia.aggregation.stages.Redact;
import dev.morphia.aggregation.stages.ReplaceRoot;
import dev.morphia.aggregation.stages.ReplaceWith;
import dev.morphia.aggregation.stages.Sample;
import dev.morphia.aggregation.stages.Set;
import dev.morphia.aggregation.stages.SetWindowFields;
import dev.morphia.aggregation.stages.Skip;
import dev.morphia.aggregation.stages.Sort;
import dev.morphia.aggregation.stages.SortByCount;
import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.aggregation.stages.Unset;
import dev.morphia.aggregation.stages.Unwind;
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
            codecs.put(Set.class, new SetStageCodec(datastore));
            codecs.put(SetWindowFields.class, new SetWindowFieldsCodec(datastore));
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
