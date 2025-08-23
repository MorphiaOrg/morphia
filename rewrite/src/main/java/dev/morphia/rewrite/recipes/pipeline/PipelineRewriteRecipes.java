package dev.morphia.rewrite.recipes.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.morphia.Datastore;
import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.Aggregation;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.rewrite.recipes.MultiMethodMatcher;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Array;
import org.openrewrite.java.tree.JavaType.Method;

import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;

public class PipelineRewriteRecipes extends Recipe {
    public static final String AGGREGATION = Aggregation.class.getName();
    private static final JavaType AGGREGATION_TYPE = JavaType.buildType(AGGREGATION);
    public static final String DATASTORE = Datastore.class.getName();

    public static final MethodMatcher AGGREGATE_ANYTHING = new MultiMethodMatcher(
            methodMatcher(DATASTORE, "aggregate(..)"),
            methodMatcher(DATASTORE + "Impl", "aggregate(..)"),
            methodMatcher(MorphiaDatastore.class.getTypeName(), "aggregate(..)"));

    private static final MethodMatcher PIPELINE = new MethodMatcher(AGGREGATION + " pipeline(..)");
    private static final Array STAGE_ARRAY_TYPE = new Array(null,
            JavaType.buildType(Stage.class.getName()), null);

    public static @NotNull <T> T javaType(Class<?> type, Class<?>... parameterTypes) {
        JavaType.Class javaType = (JavaType.Class) JavaType.buildType(type.getName());
        if (parameterTypes.length != 0) {
            javaType = javaType.withTypeParameters(
                    Arrays.stream(parameterTypes)
                            .map(t -> (JavaType) javaType(t))
                            .toList());
        }
        return (T) javaType;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline rewrite recipes";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...).";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AlternateAggregationCollection(),
                new PipelineRewrite(),
                new PipelineMergeRewrite(),
                new PipelineOutRewrite(),
                new PipelineExecuteRewrite());
    }

    public static Expression addStage(MethodInvocation invocation, MethodInvocation stage) {
        var pipeline = findPipeline(invocation);
        if (pipeline != null) {
            List<Expression> arguments = pipeline.getArguments();
            var list = new ArrayList<Expression>();
            list.addAll(arguments);
            list.add(stage);
            return pipeline
                    .withArguments(list);
        } else {
            Method method = invocation.getMethodType()
                    .withName("pipeline")
                    .withParameterNames(List.of("stages"))
                    .withParameterTypes(List.of(STAGE_ARRAY_TYPE))
                    .withReturnType(AGGREGATION_TYPE);
            return invocation.withName(invocation.getName()
                    .withSimpleName("pipeline")
                    .withType(method))
                    .withArguments(List.of(stage))
                    .withMethodType(method);
        }
    }

    private static MethodInvocation findPipeline(Expression start) {
        if (start instanceof MethodInvocation invocation) {
            if (PIPELINE.matches(invocation)) {
                return invocation;
            } else {
                return findPipeline(invocation.getSelect());
            }
        }
        return null;
    }

}
