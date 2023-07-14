package dev.morphia.aggregation.codecs;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.lang.Nullable;

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

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AggregationCodecProvider() {
        expressionCodec = new ExpressionCodec();
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
            addCodec(new AddFieldsCodec(),
                    new AutoBucketCodec(),
                    new BucketCodec(),
                    new ChangeStreamCodec(),
                    new CollectionStatsCodec(),
                    new CountCodec(),
                    new CurrentOpCodec(),
                    new DensifyCodec(),
                    new DocumentsCodec(),
                    new FacetCodec(),
                    new FillCodec(),
                    new GeoNearCodec(),
                    new GraphLookupCodec(),
                    new GroupCodec(),
                    new IndexStatsCodec(),
                    new MergeCodec(),
                    new PlanCacheStatsCodec(),
                    new LimitCodec(),
                    new LookupCodec(),
                    new MatchCodec(),
                    new OutCodec(),
                    new ProjectionCodec(),
                    new RedactCodec(),
                    new ReplaceRootCodec(),
                    new ReplaceWithCodec(),
                    new SampleCodec(),
                    new SetStageCodec(),
                    new SetWindowFieldsCodec(),
                    new SkipCodec(),
                    new SortCodec(),
                    new SortByCountCodec(),
                    new UnionWithCodec(),
                    new UnsetCodec(),
                    new UnwindCodec());
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
