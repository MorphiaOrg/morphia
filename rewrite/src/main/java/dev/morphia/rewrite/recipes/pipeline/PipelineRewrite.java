package dev.morphia.rewrite.recipes.pipeline;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.MorphiaDatastore;
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
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.Space;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.DATASTORE;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;
import static java.util.Collections.emptyList;

public class PipelineRewrite extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRewrite.class);

    static final List<MethodMatcher> matchers = List.of(
            methodMatcher(AGGREGATION, "addFields(..)"),
            methodMatcher(AGGREGATION, "autoBucket(..)"),
            methodMatcher(AGGREGATION, "bucket(..)"),
            methodMatcher(AGGREGATION, "changeStream(..)"),
            methodMatcher(AGGREGATION, "collStats(..)"),
            methodMatcher(AGGREGATION, "count(..)"),
            methodMatcher(AGGREGATION, "currentOp(..)"),
            methodMatcher(AGGREGATION, "densify(..)"),
            methodMatcher(AGGREGATION, "documents(..)"),
            methodMatcher(AGGREGATION, "facet(..)"),
            methodMatcher(AGGREGATION, "fill(..)"),
            methodMatcher(AGGREGATION, "geoNear(..)"),
            methodMatcher(AGGREGATION, "graphLookup(..)"),
            methodMatcher(AGGREGATION, "group(..)"),
            methodMatcher(AGGREGATION, "indexStats(..)"),
            methodMatcher(AGGREGATION, "limit(..)"),
            methodMatcher(AGGREGATION, "lookup(..)"),
            methodMatcher(AGGREGATION, "match(..)"),
            methodMatcher(AGGREGATION, "planCacheStats(..)"),
            methodMatcher(AGGREGATION, "project(..)"),
            methodMatcher(AGGREGATION, "redact(..)"),
            methodMatcher(AGGREGATION, "replaceRoot(..)"),
            methodMatcher(AGGREGATION, "replaceWith(..)"),
            methodMatcher(AGGREGATION, "sample(..)"),
            methodMatcher(AGGREGATION, "set(..)"),
            methodMatcher(AGGREGATION, "setWindowFields(..)"),
            methodMatcher(AGGREGATION, "skip(..)"),
            methodMatcher(AGGREGATION, "sort(..)"),
            methodMatcher(AGGREGATION, "sortByCount(..)"),
            methodMatcher(AGGREGATION, "unionWith(..)"),
            methodMatcher(AGGREGATION, "unset(..)"),
            methodMatcher(AGGREGATION, "unwind(..)"));

    private static final MethodMatcher PIPELINE = methodMatcher(AGGREGATION, "pipeline(..)");
    private static final MethodMatcher EXECUTE = methodMatcher(AGGREGATION, "execute()");
    private static final MethodMatcher TO_LIST = methodMatcher(AGGREGATION, "toList()");
    private static final MethodMatcher MEGA_MATCHER = new MethodMatcher(
            AGGREGATION + " addFields(..)") {
        @Override
        public boolean matches(@Nullable MethodCall methodCall) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(methodCall));
        }
    };

    public static final List<MethodMatcher> AGGREGATES = List.of(
            methodMatcher(DATASTORE, "aggregate(..)"),
            methodMatcher(MorphiaDatastore.class.getTypeName(), "aggregate(..)"));

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
        return new PipelineRewriteVisitor();
    }

    public static String getIndent(Space after) {
        var whitespace = after.getWhitespace();
        if (!whitespace.startsWith("\n")) {
            whitespace = "\n" + whitespace;
        }
        return whitespace + "    ";
    }

    private static class PipelineRewriteVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original,
                @NotNull ExecutionContext context) {
            MethodInvocation invocation = super.visitMethodInvocation(original, context);
            if (MEGA_MATCHER.matches(invocation)) {
                LOG.debug("matched: {}", invocation);
                var components = new Components();
                bucket(components, invocation);
                Space space = Space.build(getIndent(invocation.getPadding().getSelect().getAfter()), emptyList());
                components.arguments = components.arguments.stream()
                        .map(a -> (Expression) a.withPrefix(space))
                        .toList();

                var pipeline = synthesizePipeline(components, original);
                Expression updated = null;
                for (MethodInvocation methodInvocation : components.terminal) {
                    updated = updated == null ? methodInvocation : methodInvocation.withSelect(updated);
                }

                MethodInvocation methodInvocation = maybeAutoFormat(original, pipeline, context);
                LOG.debug("now method is: {}", methodInvocation);
                return methodInvocation;
            } else {
                return super.visitMethodInvocation(invocation, context);
            }
        }

        public MethodInvocation synthesizePipeline(Components components, @NotNull MethodInvocation invocation) {
            return invocation
                    .withName(invocation.getName().withSimpleName("pipeline"))
                    .withSelect(components.initial)
                    .withArguments(components.arguments);
        }

        private void bucket(Components components, Expression expression) {
            if (TO_LIST.matches(expression) || EXECUTE.matches(expression)) {
                MethodInvocation method = (MethodInvocation) expression;
                components.terminal.add(method);
                bucket(components, method.getSelect());
            } else if (MEGA_MATCHER.matches(expression) || PIPELINE.matches(expression)) {
                bucket(components, ((MethodInvocation) expression).getSelect());
                collectArguments(components.arguments, (MethodInvocation) expression);
            } else if (PipelineRewriteRecipes.AGGREGATE_ANYTHING.matches(expression) || expression instanceof Identifier) {
                components.initial = expression;
            } else {
                throw new UnsupportedOperationException(expression.toString());
            }
        }

        private void collectArguments(List<Expression> args, MethodInvocation invocation) {
            if (COLLECTORS.stream().noneMatch(collector -> collector.matches(this, args, invocation))) {
                args.addAll(invocation.getArguments());
            }
        }

        private Expression rollup(Expression invocation) {
            if (invocation instanceof MethodInvocation methodInvocation && MEGA_MATCHER.matches(methodInvocation)) {
                var processed = rollup(methodInvocation.getSelect());
                if (PipelineRewriteRecipes.AGGREGATE_ANYTHING.matches(processed)) {
                    return processed;
                }
            } else if (PipelineRewriteRecipes.AGGREGATE_ANYTHING.matches(invocation)) {
                return invocation;
            }
            return null;
        }

        private class Components {
            final List<MethodInvocation> terminal = new ArrayList<>();
            List<Expression> arguments = new ArrayList<>();
            Expression initial;
        }
    }
}
