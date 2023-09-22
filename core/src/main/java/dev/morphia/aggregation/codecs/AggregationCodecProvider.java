package dev.morphia.aggregation.codecs;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.lang.Nullable;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.codecs.stages.AddFieldsCodec;
import dev.morphia.aggregation.codecs.stages.AutoBucketCodec;
import dev.morphia.aggregation.codecs.stages.BucketCodec;
import dev.morphia.aggregation.codecs.stages.ChangeStreamCodec;
import dev.morphia.aggregation.codecs.stages.CollectionStatsCodec;
import dev.morphia.aggregation.codecs.stages.CountCodec;
import dev.morphia.aggregation.codecs.stages.CurrentOpCodec;
import dev.morphia.aggregation.codecs.stages.DensifyCodec;
import dev.morphia.aggregation.codecs.stages.DocumentsCodec;
import dev.morphia.aggregation.codecs.stages.FacetCodec;
import dev.morphia.aggregation.codecs.stages.FillCodec;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AggregationCodecProvider implements CodecProvider {

    private final Codec expressionCodec;
    private Map<Class, StageCodec> codecs;
    private MorphiaDatastore datastore;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregationCodecProvider(MorphiaDatastore datastore) {
        this.datastore = datastore;
        expressionCodec = new ExpressionCodec(datastore);
    }

    @Override
    @Nullable
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
            addCodec(new AddFieldsCodec(datastore),
                    new AutoBucketCodec(datastore),
                    new BucketCodec(datastore),
                    new ChangeStreamCodec(datastore),
                    new CollectionStatsCodec(datastore),
                    new CountCodec(datastore),
                    new CurrentOpCodec(datastore),
                    new DensifyCodec(datastore),
                    new DocumentsCodec(datastore),
                    new FacetCodec(datastore),
                    new FillCodec(datastore),
                    new GeoNearCodec(datastore),
                    new GraphLookupCodec(datastore),
                    new GroupCodec(datastore),
                    new IndexStatsCodec(datastore),
                    new MergeCodec(datastore),
                    new PlanCacheStatsCodec(datastore),
                    new LimitCodec(datastore),
                    new LookupCodec(datastore),
                    new MatchCodec(datastore),
                    new OutCodec(datastore),
                    new ProjectionCodec(datastore),
                    new RedactCodec(datastore),
                    new ReplaceRootCodec(datastore),
                    new ReplaceWithCodec(datastore),
                    new SampleCodec(datastore),
                    new SetStageCodec(datastore),
                    new SetWindowFieldsCodec(datastore),
                    new SkipCodec(datastore),
                    new SortCodec(datastore),
                    new SortByCountCodec(datastore),
                    new UnionWithCodec(datastore),
                    new UnsetCodec(datastore),
                    new UnwindCodec(datastore));
        }
        return codecs;
    }

    @Nullable
    private void addCodec(StageCodec... stageCodecs) {
        for (StageCodec codec : stageCodecs) {
            codecs.put(codec.getEncoderClass(), codec);
        }
    }
}
