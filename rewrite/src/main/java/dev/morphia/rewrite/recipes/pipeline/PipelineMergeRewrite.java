package dev.morphia.rewrite.recipes.pipeline;

import java.util.List;
import java.util.stream.Collectors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.AGGREGATION;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.STRING_TYPE;
import static dev.morphia.rewrite.recipes.PipelineRewriteRecipes.addStage;
import static dev.morphia.rewrite.recipes.RewriteUtils.findMorphiaCore;

public class PipelineMergeRewrite extends Recipe {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineMergeRewrite.class);

    private static final MethodMatcher MERGE = new MethodMatcher(AGGREGATION + " merge(..)");

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

                if (MERGE.matches(original)) {
                    MethodInvocation invocation = super.visitMethodInvocation(original, context);
                    LOG.debug("PipelineMergeRewrite matches");
                    LOG.debug("invocation = {}", invocation);
                    var arguments = invocation.getArguments();
                    var mergeInfo = extractTarget((MethodInvocation) arguments.get(0));
                    invocation = maybeAutoFormat(original, (MethodInvocation) addStage(invocation, mergeInfo), context);
                    LOG.debug("now invocation = {}", invocation);
                    return invocation;
                }

                return original;
            }

            private MethodInvocation extractTarget(MethodInvocation originalArguments) {

                maybeRemoveImport("dev.morphia.aggregation.stages.Merge.into");
                var arguments = originalArguments.getArguments();
                var parameters = arguments.stream()
                        .map(a -> a.getType().toString())
                        .map("#{any(%s)}"::formatted)
                        .collect(Collectors.joining(","));
                MethodInvocation applied = (JavaTemplate.builder("Merge.merge(%s)".formatted(parameters)))
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath(List.of(findMorphiaCore().toPath())))
                        .imports(Merge.class.getName())
                        .build().apply(new Cursor(getCursor(), originalArguments),
                                originalArguments.getCoordinates().replace(), arguments.toArray());

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
                        .withMethodType(method);
            }
        };
    }

}
