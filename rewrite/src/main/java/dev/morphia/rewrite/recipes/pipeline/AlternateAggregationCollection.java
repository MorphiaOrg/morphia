package dev.morphia.rewrite.recipes.pipeline;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.AggregationOptions;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.DATASTORE;
import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaCore;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;
import static org.openrewrite.java.JavaParser.fromJavaVersion;

public class AlternateAggregationCollection extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(AlternateAggregationCollection.class);
    public static final List<MethodMatcher> AGGREGATES = List.of(
            methodMatcher(DATASTORE, "aggregate(String)"),
            methodMatcher(MorphiaDatastore.class.getTypeName(), "aggregate(String)"));

    public static final MethodMatcher AGGREGATE = new MethodMatcher(
            AGGREGATION + " addFields(..)") {
        @Override
        public boolean matches(@Nullable MethodCall methodCall) {
            return AGGREGATES.stream().anyMatch(matcher -> matcher.matches(methodCall));
        }
    };

    @Override
    public @DisplayName String getDisplayName() {
        return "Alternate aggregation collection";
    }

    @Override
    public @Description String getDescription() {
        return "Moves the alternate collection name to a call to AggregationOptions.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AlternateCollectionVisitor();
    }

    private static class AlternateCollectionVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public MethodInvocation visitMethodInvocation(MethodInvocation method, ExecutionContext executionContext) {
            if (AGGREGATE.matches(method)) {
                LOG.debug("method matches:  {}", method);
                var template = JavaTemplate.builder("new AggregationOptions().collection(#{any()})")
                        .javaParser(fromJavaVersion()
                                .classpath(List.of(findMorphiaCore().toPath())))
                        .imports(AggregationOptions.class.getName())
                        .build();

                maybeAddImport(AggregationOptions.class.getName());
                MethodInvocation updated = maybeAutoFormat(method,
                        template.apply(getCursor(), method.getCoordinates().replaceArguments(), method.getArguments().get(0)),
                        executionContext);
                LOG.debug("method updated:  {}", updated);
                return updated;
            } else {
                return super.visitMethodInvocation(method, executionContext);
            }
        }
    }
}
