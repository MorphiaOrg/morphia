package dev.morphia.rewrite.recipes;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;

public class PipelineRewrite extends Recipe {

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
        var matchers = List.of(
                new MethodMatcher("dev.morphia.aggregation.Aggregation addFields(dev.morphia.aggregation.stages.AddFields)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation autoBucket(dev.morphia.aggregation.stages.AutoBucket)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation bucket(dev.morphia.aggregation.stages.Bucket)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation changeStream()"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation changeStream(dev.morphia.aggregation.stages.ChangeStream)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation collStats(dev.morphia.aggregation.stages.CollectionStats)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation count(dev.morphia.aggregation.stages.Count)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation currentOp(dev.morphia.aggregation.stages.CountOp)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation densify(dev.morphia.aggregation.stages.Densify)"),
                new MethodMatcher(
                        "dev.morphia.aggregation.Aggregation documents(dev.morphia.aggregation.expressions.impls.DocumentExpression)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation facet(dev.morphia.aggregation.stages.Facet)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation fill(dev.morphia.aggregation.stages.Fill)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation geoNear(dev.morphia.aggregation.stages.GeoNear)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation graphLookup(dev.morphia.aggregation.stages.GraphLookup)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation group(dev.morphia.aggregation.stages.Group)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation indexStats(dev.morphia.aggregation.stages.IndexStats)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation limit(long)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation lookup(dev.morphia.aggregation.stages.Lookup)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation match(dev.morphia.aggregation.stages.Match)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation planCacheStats()"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation project(dev.morphia.aggregation.stages.Projection)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation redact(dev.morphia.aggregation.stages.Redact)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation replaceRoot(dev.morphia.aggregation.stages.ReplaceRoot)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation replaceWith(dev.morphia.aggregation.stages.ReplaceWith)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation sample(dev.morphia.aggregation.stages.Sample)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation set(dev.morphia.aggregation.stages.Set)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation skip(long)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation sort(dev.morphia.aggregation.stages.Sort)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation sortByCount(dev.morphia.aggregation.stages.SortByCount)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation unionWith(Class,Stage...)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation unionWith(String,Stage...)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation unset(dev.morphia.aggregation.stages.Unset)"),
                new MethodMatcher("dev.morphia.aggregation.Aggregation unwind(dev.morphia.aggregation.stages.Unwind)"));

        return new JavaIsoVisitor<>() {

            @Override
            public MethodInvocation visitMethodInvocation(MethodInvocation method, @NotNull ExecutionContext context) {
                if (matchers.stream().anyMatch(matcher->matcher.matches(method))) {
                    System.out.println("method = " + method);
                    Expression expression = method.getArguments().get(0);
                    System.out.println("expression = " + expression);
                    method.withName()
                    return super.visitMethodInvocation(method, context);
                } else {
                    return super.visitMethodInvocation(method, context);
                }
            }
        };
    }
}
