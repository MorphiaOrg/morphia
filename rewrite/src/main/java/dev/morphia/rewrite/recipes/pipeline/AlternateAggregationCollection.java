package dev.morphia.rewrite.recipes.pipeline;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.rewrite.recipes.MultiMethodMatcher;
import dev.morphia.rewrite.recipes.PipelineRewriteRecipes;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.DATASTORE;
import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaCore;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;
import static java.util.List.of;
import static org.openrewrite.java.JavaParser.fromJavaVersion;

public class AlternateAggregationCollection extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(AlternateAggregationCollection.class);

    public static final MethodMatcher AGGREGATE = new MultiMethodMatcher(
            methodMatcher(DATASTORE, "aggregate(java.lang.String)"),
            methodMatcher(DATASTORE + "Impl", "aggregate(java.lang.String)"),
            methodMatcher(MorphiaDatastore.class.getTypeName(), "aggregate(java.lang.String)"),
            methodMatcher(DATASTORE, "aggregate(kotlin.String)"),
            methodMatcher(DATASTORE + "Impl", "aggregate(kotlin.String)"),
            methodMatcher(MorphiaDatastore.class.getTypeName(), "aggregate(kotlin.String)"));

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

                maybeAddImport(Document.class.getName(), false);
                maybeAddImport(AggregationOptions.class.getName(), false);
                var newType = method.getMethodType()
                        .withParameterTypes(of(PipelineRewriteRecipes.<JavaType> javaType(AggregationOptions.class)));
                MethodInvocation updated = buildTemplate()
                        .apply(getCursor(), method.getCoordinates().replaceArguments(),
                                method.getArguments().get(0));
                updated = updated.withMethodType(newType)
                        .withName(updated.getName().withType(newType))
                        .withSelect(method.getSelect());
                updated = maybeAutoFormat(method, updated, executionContext);
                LOG.debug("method updated:  {}", updated);
                return updated;
            } else {
                return super.visitMethodInvocation(method, executionContext);
            }
        }
    }

    private static @NotNull JavaTemplate buildTemplate() {
        return JavaTemplate.builder("new AggregationOptions().collection(#{any()})")
                .javaParser(fromJavaVersion()
                        .classpath(of(findMorphiaCore())))
                .imports(AggregationOptions.class.getName())
                .build();
    }
}
