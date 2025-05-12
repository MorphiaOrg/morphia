package dev.morphia.rewrite.recipes.pipeline;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.aggregation.stages.Count;
import dev.morphia.aggregation.stages.IndexStats;
import dev.morphia.aggregation.stages.Limit;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.aggregation.stages.PlanCacheStats;
import dev.morphia.aggregation.stages.Sample;
import dev.morphia.aggregation.stages.Skip;
import dev.morphia.aggregation.stages.SortByCount;
import dev.morphia.aggregation.stages.Stage;
import dev.morphia.aggregation.stages.UnionWith;
import dev.morphia.query.filters.Filter;
import dev.morphia.rewrite.recipes.PipelineRewriteRecipes;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.Space;

import static java.util.Collections.emptyList;

public class PipelineRewrite extends Recipe {
    static final List<MethodMatcher> matchers = List.of(
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " pipeline(..)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " addFields(dev.morphia.aggregation.stages.AddFields)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " autoBucket(dev.morphia.aggregation.stages.AutoBucket)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " bucket(dev.morphia.aggregation.stages.Bucket)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " changeStream()"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " changeStream(dev.morphia.aggregation.stages.ChangeStream)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " collStats(dev.morphia.aggregation.stages.CollectionStats)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " count(..)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " currentOp(dev.morphia.aggregation.stages.CurrentOp)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " densify(dev.morphia.aggregation.stages.Densify)"),
            new MethodMatcher(
                    PipelineRewriteRecipes.AGGREGATION + " documents(dev.morphia.aggregation.expressions.impls.DocumentExpression[])"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " facet(dev.morphia.aggregation.stages.Facet)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " fill(dev.morphia.aggregation.stages.Fill)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " geoNear(dev.morphia.aggregation.stages.GeoNear)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " graphLookup(dev.morphia.aggregation.stages.GraphLookup)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " group(dev.morphia.aggregation.stages.Group)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " indexStats()"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " limit(..)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " lookup(dev.morphia.aggregation.stages.Lookup)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " match(dev.morphia.query.filters.Filter...)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " planCacheStats()"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " project(dev.morphia.aggregation.stages.Projection)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " redact(dev.morphia.aggregation.stages.Redact)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " replaceRoot(dev.morphia.aggregation.stages.ReplaceRoot)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " replaceWith(dev.morphia.aggregation.stages.ReplaceWith)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " sample(long)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " set(dev.morphia.aggregation.stages.AddFields)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " set(dev.morphia.aggregation.stages.Set)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " setWindowFields(dev.morphia.aggregation.stages.SetWindowFields)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " skip(long)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " sort(dev.morphia.aggregation.stages.Sort)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " sortByCount(dev.morphia.aggregation.stages.SortByCount)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " sortByCount(dev.morphia.aggregation.expressions.impls.Expression)"),
            new MethodMatcher(
                    PipelineRewriteRecipes.AGGREGATION
                            + " unionWith(Class,dev.morphia.aggregation.stages.Stage,dev.morphia.aggregation.stages.Stage[])"),
            new MethodMatcher(
                    PipelineRewriteRecipes.AGGREGATION
                            + " unionWith(String,dev.morphia.aggregation.stages.Stage,dev.morphia.aggregation.stages.Stage[])"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " unset(dev.morphia.aggregation.stages.Unset)"),
            new MethodMatcher(PipelineRewriteRecipes.AGGREGATION + " unwind(dev.morphia.aggregation.stages.Unwind)"));

    private static final MethodMatcher MEGA_MATCHER = new MethodMatcher(
            PipelineRewriteRecipes.AGGREGATION + " addFields(dev.morphia.aggregation.stages.AddFields)") {
        @Override
        public boolean matches(@Nullable MethodCall methodCall) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(methodCall));
        }
    };

    private static final List<ArgumentCollector> COLLECTORS = List.of(
            collector(Count.class, "count"),
            collector(IndexStats.class, "indexStats"),
            collector(Limit.class, "limit"),
            collector(Match.class, "match", Filter.class),
            collector(PlanCacheStats.class, "planCacheStats"),
            collector(Sample.class, "sample"),
            collector(Skip.class, "skip"),
            collector(SortByCount.class, "sortByCount", dev.morphia.aggregation.expressions.impls.Expression.class),
            collector(UnionWith.class, "unionWith", Stage.class));

    private static @NotNull ArgumentCollector collector(Class<?> type, String method, Class<?>... argTypes) {
        return new ArgumentCollector(type, method, argTypes);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline rewrite";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...).";
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    public int maxCycles() {
        return 2;
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<>() {
            @Override
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original,
                    @NotNull ExecutionContext context) {
                MethodInvocation invocation = super.visitMethodInvocation(original, context);
                if (MEGA_MATCHER.matches(invocation)) {
                    Expression updated = invocation;
                    List<Expression> arguments = new ArrayList<>();
                    while (MEGA_MATCHER.matches(updated)) {
                        invocation = (MethodInvocation) updated;
                        collectArguments(arguments, invocation);
                        updated = invocation.getSelect();
                    }
                    Space space = Space.build(getIndent(invocation.getPadding().getSelect().getAfter()), emptyList());
                    arguments = arguments.stream()
                            .map(a -> (Expression) a.withPrefix(space))
                            .toList();

                    invocation = invocation.withName(invocation.getName().withSimpleName("pipeline"))
                            .withArguments(arguments);

                    return maybeAutoFormat(original, invocation, context);
                } else {
                    return super.visitMethodInvocation(invocation, context);
                }
            }

            private void collectArguments(List<Expression> args, MethodInvocation invocation) {
                if (COLLECTORS.stream().noneMatch(collector -> collector.matches(this, args, invocation))) {
                    args.addAll(0, invocation.getArguments());
                }
            }
        };
    }

    public static String getIndent(Space after) {
        var whitespace = after.getWhitespace();
        if (!whitespace.startsWith("\n")) {
            whitespace = "\n" + whitespace;
        }
        return whitespace + "    ";
    }

}
