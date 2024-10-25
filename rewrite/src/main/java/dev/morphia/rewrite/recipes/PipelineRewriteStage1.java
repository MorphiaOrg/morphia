package dev.morphia.rewrite.recipes;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;

public class PipelineRewriteStage1 extends Recipe {

    static final String AGGREGATION = "dev.morphia.aggregation.Aggregation";
    static final MethodMatcher pipeline = new MethodMatcher(PipelineRewriteStage1.AGGREGATION + " pipeline(..)");
    static final List<MethodMatcher> matchers = List.of(
            new MethodMatcher(AGGREGATION + " addFields(dev.morphia.aggregation.stages.AddFields)"),
            new MethodMatcher(AGGREGATION + " autoBucket(dev.morphia.aggregation.stages.AutoBucket)"),
            new MethodMatcher(AGGREGATION + " bucket(dev.morphia.aggregation.stages.Bucket)"),
            new MethodMatcher(AGGREGATION + " changeStream()"),
            new MethodMatcher(AGGREGATION + " changeStream(dev.morphia.aggregation.stages.ChangeStream)"),
            new MethodMatcher(AGGREGATION + " collStats(dev.morphia.aggregation.stages.CollectionStats)"),
            new MethodMatcher(AGGREGATION + " count(dev.morphia.aggregation.stages.Count)"),
            new MethodMatcher(AGGREGATION + " currentOp(dev.morphia.aggregation.stages.CountOp)"),
            new MethodMatcher(AGGREGATION + " densify(dev.morphia.aggregation.stages.Densify)"),
            new MethodMatcher(AGGREGATION + " documents(dev.morphia.aggregation.expressions.impls.DocumentExpression)"),
            new MethodMatcher(AGGREGATION + " facet(dev.morphia.aggregation.stages.Facet)"),
            new MethodMatcher(AGGREGATION + " fill(dev.morphia.aggregation.stages.Fill)"),
            new MethodMatcher(AGGREGATION + " geoNear(dev.morphia.aggregation.stages.GeoNear)"),
            new MethodMatcher(AGGREGATION + " graphLookup(dev.morphia.aggregation.stages.GraphLookup)"),
            new MethodMatcher(AGGREGATION + " group(dev.morphia.aggregation.stages.Group)"),
            new MethodMatcher(AGGREGATION + " indexStats(dev.morphia.aggregation.stages.IndexStats)"),
            new MethodMatcher(AGGREGATION + " limit(long)"),
            new MethodMatcher(AGGREGATION + " lookup(dev.morphia.aggregation.stages.Lookup)"),
            new MethodMatcher(AGGREGATION + " match(dev.morphia.aggregation.stages.Match)"),
            new MethodMatcher(AGGREGATION + " planCacheStats()"),
            new MethodMatcher(AGGREGATION + " project(dev.morphia.aggregation.stages.Projection)"),
            new MethodMatcher(AGGREGATION + " redact(dev.morphia.aggregation.stages.Redact)"),
            new MethodMatcher(AGGREGATION + " replaceRoot(dev.morphia.aggregation.stages.ReplaceRoot)"),
            new MethodMatcher(AGGREGATION + " replaceWith(dev.morphia.aggregation.stages.ReplaceWith)"),
            new MethodMatcher(AGGREGATION + " sample(dev.morphia.aggregation.stages.Sample)"),
            new MethodMatcher(AGGREGATION + " set(dev.morphia.aggregation.stages.Set)"),
            new MethodMatcher(AGGREGATION + " skip(long)"),
            new MethodMatcher(AGGREGATION + " sort(dev.morphia.aggregation.stages.Sort)"),
            new MethodMatcher(AGGREGATION + " sortByCount(dev.morphia.aggregation.stages.SortByCount)"),
            new MethodMatcher(AGGREGATION + " unionWith(Class,Stage...)"),
            new MethodMatcher(AGGREGATION + " unionWith(String,Stage...)"),
            new MethodMatcher(AGGREGATION + " unset(dev.morphia.aggregation.stages.Unset)"),
            new MethodMatcher(AGGREGATION + " unwind(dev.morphia.aggregation.stages.Unwind)"));

    @Override
    public String getDisplayName() {
        return "Aggregation pipeline rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites an aggregation from using stage-named methods to using pipeline(Stage...).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
                return working(methodInvocation, context);
            }

            public MethodInvocation working(MethodInvocation methodInvocation, @NotNull ExecutionContext context) {
                if (matchers.stream().anyMatch(matcher -> matcher.matches(methodInvocation))) {
                    return super.visitMethodInvocation(methodInvocation
                            .withName(methodInvocation.getName().withSimpleName("pipeline")),
                            context);
                } else {
                    return super.visitMethodInvocation(methodInvocation, context);
                }
            }

        };
    }
}
