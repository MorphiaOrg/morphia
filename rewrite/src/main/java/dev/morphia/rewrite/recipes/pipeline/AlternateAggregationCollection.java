package dev.morphia.rewrite.recipes.pipeline;

import java.util.List;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.AggregationOptions;
import dev.morphia.rewrite.recipes.MultiMethodMatcher;

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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaDependencies;
import static dev.morphia.rewrite.recipes.RewriteUtils.methodMatcher;
import static dev.morphia.rewrite.recipes.pipeline.PipelineRewriteRecipes.DATASTORE;
import static dev.morphia.rewrite.recipes.pipeline.PipelineRewriteRecipes.javaType;
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

                // For Java, add static import for aggregationOptions()
                if (!isKotlin(method)) {
                    maybeAddImport(AggregationOptions.class.getName(), "aggregationOptions", false);
                }

                var newType = method.getMethodType()
                        .withParameterTypes(of(javaType(Class.class), javaType(AggregationOptions.class)));

                var classLiteral = documentLiteral(method);

                // Create the AggregationOptions argument: aggregationOptions().collection("collectionName")
                // Use coordinates that don't create a cycle
                MethodInvocation aggregationOptionsArg = templateWithStaticImport(method, "AggregationOptions().collection(#{any()})",
                        "AggregationOptions.aggregationOptions().collection(#{any()})", of(AggregationOptions.class),
                        AggregationOptions.class.getName() + ".aggregationOptions")
                        .apply(getCursor().getParent(), method.getCoordinates().replaceArguments(),
                                method.getArguments().get(0));

                // For Java, simplify the static method call to use the static import
                if (!isKotlin(method) && aggregationOptionsArg.getSelect() instanceof MethodInvocation aggregationOptionsCall) {
                    // Remove the class qualifier from aggregationOptions() to use the static import
                    aggregationOptionsArg = aggregationOptionsArg.withSelect(aggregationOptionsCall.withSelect(null));
                }

                // Build the new arguments list: [Document.class, aggregationOptions().collection(...)]
                var args = List.of(classLiteral, aggregationOptionsArg);

                // Transform the original aggregate method to have the new signature and arguments
                MethodInvocation updated = method.withMethodType(newType)
                        .withName(method.getName().withType(newType))
                        .withArguments(args);
                updated = maybeAutoFormat(method, updated, executionContext);
                LOG.debug("method updated:  {}", updated);
                return updated;
            } else {
                return super.visitMethodInvocation(method, executionContext);
            }
        }

        private @NotNull Expression documentLiteral(MethodInvocation method) {
            return ((MethodInvocation) template(method, "Document::class.java", "Document.class", of(Document.class))
                    .apply(getCursor(), method.getCoordinates().replaceArguments())).getArguments().get(0);
        }
    }

    private static @NotNull JavaTemplate template(MethodInvocation method, String kotlinCode, String javaCode, List<Class<?>> imports) {
        return template(builder(method, kotlinCode, javaCode), imports);
    }

    private static @NotNull JavaTemplate templateWithStaticImport(MethodInvocation method, String kotlinCode, String javaCode,
            List<Class<?>> imports, String staticImport) {
        var builder = builder(method, kotlinCode, javaCode);
        if (!isKotlin(method)) {
            builder = builder.staticImports(staticImport);
        }
        return template(builder, imports);
    }

    private static @NotNull JavaTemplate template(String javaCode, List<Class<?>> imports) {
        return template(JavaTemplate.builder(javaCode), imports);
    }

    private static @NotNull JavaTemplate template(JavaTemplate.Builder builder, List<Class<?>> imports) {
        return builder
                .imports(imports.stream().map(c -> c.getName()).toArray(String[]::new))
                .build();
    }

    private static JavaTemplate.@NotNull Builder builder(MethodInvocation method, String kotlinCode, String javaCode) {
        return isKotlin(method) ? KotlinTemplate.builder(kotlinCode)
                .parser(KotlinParser.builder()
                        .classpath(findMorphiaDependencies()))
                : JavaTemplate.builder(javaCode)
                        .javaParser(fromJavaVersion()
                                .classpath(findMorphiaDependencies()));
    }

    private static boolean isKotlin(MethodInvocation method) {
        return method.getMethodType().getParameterTypes().stream().anyMatch(type -> type.toString().contains("kotlin"));
    }
}
