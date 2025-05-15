package dev.morphia.rewrite.recipes.pipeline;

import java.util.List;

import dev.morphia.aggregation.stages.Merge;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.FullyQualified;
import org.openrewrite.java.tree.JavaType.Method;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.*;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.addStage;
import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaCore;

public class PipelineMergeRewrite extends Recipe {

    private static final MethodMatcher MERGE = new MethodMatcher(AGGREGATION + " merge(..)");

    private static final JavaTemplate TEMPLATE = (JavaTemplate.builder("Merge.merge()"))
            .javaParser(JavaParser.fromJavaVersion()
                    .classpath(List.of(findMorphiaCore().toPath())))
            .imports(Merge.class.getName())
            .build();

    @Override
    public @NotNull String getDisplayName() {
        return "Aggregation pipeline merge() rewrite";
    }

    @Override
    public @NotNull String getDescription() {
        return "Rewrites uses of merge() moving the parameters in to pipeline().";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public @NotNull MethodInvocation visitMethodInvocation(@NotNull MethodInvocation original, @NotNull ExecutionContext context) {
                MethodInvocation invocation = super.visitMethodInvocation(original, context);

                if (MERGE.matches(invocation)) {
                    var arguments = invocation.getArguments();
                    var mergeInfo = extractTarget((MethodInvocation) arguments.get(0));
                    invocation = (MethodInvocation) addStage(invocation, mergeInfo);
                }

                return invocation;
            }

            private MethodInvocation extractTarget(MethodInvocation originalArguments) {

                maybeRemoveImport("dev.morphia.aggregation.stages.Merge.into");
                MethodInvocation applied = TEMPLATE.apply(new Cursor(getCursor(), originalArguments),
                        originalArguments.getCoordinates().replace());

                var arguments = originalArguments.getArguments();
                List<String> parameterNames = arguments.size() == 1 ? List.of("collection") : List.of("database", "collection");
                List<JavaType> parameterTypes = arguments.size() == 1 ? List.of(STRING_TYPE) : List.of(STRING_TYPE, STRING_TYPE);

                Method method = new Method(null, 1,
                        (FullyQualified) JavaType.buildType(Merge.class.getName()),
                        "merge",
                        JavaType.buildType(Merge.class.getName()),
                        parameterNames,
                        parameterTypes,
                        null, null, null, null);

                return applied.withName(applied.getName().withType(method))
                        .withArguments(arguments)
                        .withMethodType(method);
            }
        };
    }

}
